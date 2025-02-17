package com.transferwise.common.gaffer;

import com.transferwise.common.gaffer.util.Clock;
import com.transferwise.common.gaffer.util.DummyXaResource;
import com.transferwise.common.gaffer.util.ExceptionThrower;
import com.transferwise.common.gaffer.util.HeuristicMixedExceptionImpl;
import com.transferwise.common.gaffer.util.RollbackExceptionImpl;
import com.transferwise.common.gaffer.util.TransactionStatuses;
import com.transferwise.common.gaffer.util.Uid;
import com.transferwise.common.gaffer.util.UidImpl;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.Transaction;
import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GafferTransaction implements Transaction {

  private int status = Status.STATUS_NO_TRANSACTION;
  @Getter
  private final Uid globalTransactionId;
  private final List<XAResource> xaResources = new CopyOnWriteArrayList<>();
  private final List<Synchronization> synchronizations = new CopyOnWriteArrayList<>();
  private final List<Synchronization> interposedSynchronizations = new CopyOnWriteArrayList<>();
  private final Map<Object, Object> resources = new ConcurrentHashMap<>();
  @Getter
  private final long startTimeMillis;
  private long beforeCommitValidationRequiredTimeMs = -1;
  @Setter
  @Getter
  private long timeoutMillis = -1;
  private boolean wasSuspended;
  private final ExceptionThrower exceptionThrower;
  private final MetricsTemplate metricsTemplate;
  private AbandonedTransactionsTracker abandonedTransactionsTracker;
  private final Clock clock;

  public GafferTransaction(Clock clock, GafferJtaProperties gafferJtaProperties, MetricsTemplate metricsTemplate) {
    this.metricsTemplate = metricsTemplate;
    this.clock = clock;

    String instanceId = gafferJtaProperties.getInstanceId();
    startTimeMillis = clock.currentTimeMillis();
    globalTransactionId = new UidImpl(instanceId, startTimeMillis);
    exceptionThrower = new ExceptionThrower(gafferJtaProperties.isLogExceptions());

    if (gafferJtaProperties.isTrackAbandonedTransactions()) {
      abandonedTransactionsTracker = new AbandonedTransactionsTracker(metricsTemplate, globalTransactionId, status);
      SingletonCleaner.INSTANCE.register(this, abandonedTransactionsTracker);
      metricsTemplate.registerTransactionAbandoningTracking();
    }
  }

  public void setSuspended(boolean suspended) {
    if (suspended) {
      wasSuspended = true;
    }
  }

  public void putResource(Object key, Object val) {
    if (key == null) {
      exceptionThrower.throwException(new IllegalArgumentException("Resource key can not be null."));
    }
    resources.put(key, val);
  }

  public Object getResource(Object key) {
    if (key == null) {
      exceptionThrower.throwException(new IllegalArgumentException("Resource key can not be null."));
    }
    return resources.get(key);
  }

  public void begin(Integer timeoutSeconds, long beforeCommitValidationRequiredTimeMs) {
    if (log.isDebugEnabled()) {
      log.debug("Starting transaction '{}' with timeout of '{}' seconds.", getTransactionInfo(),
          timeoutSeconds == null ? "infinite" : timeoutSeconds);
    }
    if (timeoutSeconds != null) {
      setTimeoutMillis(timeoutSeconds * 1000);
    }
    this.beforeCommitValidationRequiredTimeMs = beforeCommitValidationRequiredTimeMs;
    setStatusInternal(Status.STATUS_ACTIVE);
  }

  private void setStatusInternal(int status) {
    this.status = status;
    if (abandonedTransactionsTracker != null) {
      abandonedTransactionsTracker.status = status;
    }
  }

  @Override
  public void commit() throws RollbackException, IllegalStateException {
    if (log.isDebugEnabled()) {
      log.debug("Committing transaction '{}'.", getTransactionInfo());
    }
    if (status == Status.STATUS_NO_TRANSACTION) {
      exceptionThrower.throwException(new IllegalStateException("Can not commit '" + getTransactionInfo() + "'. Transaction has not been started."));
    }
    if (isDoneOrFinishing()) {
      exceptionThrower.throwException(new IllegalStateException("Can not commit '" + getTransactionInfo() + "' with status '" + status
          + "''. Transaction is finishing or finished."));
    }

    try {
      fireBeforeCompletionEvent();
    } catch (Throwable t) {
      rollback();
      exceptionThrower
          .throwException(new RollbackExceptionImpl("Can not commit '" + getTransactionInfo() + "'. Before completion event firing failed.", t));
    }

    if (status == Status.STATUS_MARKED_ROLLBACK) {
      rollback();
      exceptionThrower
          .throwException(new RollbackExceptionImpl("Can not commit '" + getTransactionInfo() + "'. Transaction was marked as to be rolled back."));
    } else if (isTimedOut()) {
      rollback();
      exceptionThrower.throwException(new RollbackExceptionImpl("Can not commit '" + getTransactionInfo() + "'. Transaction has timed out."));
    }

    if (beforeCommitValidationRequired()) {
      XAResource xaResource = null;
      try {
        for (XAResource tmpXaResource : getSortedXaResource(xaResources)) {
          xaResource = tmpXaResource;
          if (xaResource instanceof ValidatableResource) {
            if (!((ValidatableResource) xaResource).isValid()) {
              throw new IllegalStateException("Resource " + xaResource + " is not valid anymore.");
            }
          }
        }
      } catch (Throwable t) {
        rollback();
        exceptionThrower.throwException(new RollbackExceptionImpl(
            "Can not commit '" + getTransactionInfo() + "'." + (xaResource == null ? "" : " Invalid xaResource found: " + xaResource), t));
      }
    }

    try {
      setStatus(Status.STATUS_COMMITTING);

      Throwable t = null;
      int idx = 0;

      for (XAResource xaResource : getSortedXaResource(xaResources)) {
        try {
          xaResource.commit(null, true);
        } catch (Throwable t0) {
          t = t0;
          break;
        }
        idx++;
      }

      if (t != null) {
        if (idx > 0) {
          metricsTemplate.registerHeuristicTransactionCommit();
        }
        rollback();
        if (idx == 0) {
          exceptionThrower
              .throwException(new RollbackExceptionImpl("Can not commit '" + getTransactionInfo() + "'. Commiting a resource failed.", t));
        }
        exceptionThrower
            .throwException(new HeuristicMixedExceptionImpl("Can not commit '" + getTransactionInfo() + "'. Commiting a resource failed.", t));
      }
      setStatus(Status.STATUS_COMMITTED);

      if (abandonedTransactionsTracker != null) {
        abandonedTransactionsTracker.abandoned.set(false);
      }

      metricsTemplate.registerTransactionCommit(wasSuspended);
      if (log.isDebugEnabled()) {
        log.debug("Transaction '{}' successfully commited.", getTransactionInfo());
      }
    } finally {
      fireAfterCompletionEvent();
      xaResources.clear();
      resources.clear();
    }
  }

  @Override
  public boolean delistResource(XAResource xaRes, int flag) {
    if (log.isDebugEnabled()) {
      log.debug("Delisting resource '{}' for transaction '{}'.", xaRes, getTransactionInfo());
    }
    if (status == Status.STATUS_NO_TRANSACTION) {
      exceptionThrower
          .throwException(new IllegalStateException("Can not delist resource. Transaction '" + getTransactionInfo() + "' has not been started."));
    }
    if (isWorking()) {
      exceptionThrower
          .throwException(new IllegalStateException("Can not delist resource. Transaction '" + getTransactionInfo() + "' commit is in progress."));
    }
    return xaResources.remove(xaRes);
  }

  @Override
  public boolean enlistResource(XAResource xaRes) {
    if (log.isDebugEnabled()) {
      log.debug("Enlisting resource '{}' for transaction '{}'.", xaRes, getTransactionInfo());
    }
    if (status == Status.STATUS_NO_TRANSACTION) {
      exceptionThrower
          .throwException(new IllegalStateException("Can not enlist resource. Transaction '" + getTransactionInfo() + "' has not been started."));
    }
    if (status == Status.STATUS_MARKED_ROLLBACK) {
      exceptionThrower.throwException(new IllegalStateException("Can not enlist resource. Transaction '" + getTransactionInfo()
          + "' has been marked to roll back."));
    }
    if (isDoneOrFinishing()) {
      exceptionThrower
          .throwException(new IllegalStateException("Can not enlist resource. Transaction '" + getTransactionInfo() + "' is finished or finishing."));
    }
    if (!(xaRes instanceof DummyXaResource)) {
      exceptionThrower.throwException(new IllegalStateException("Full XA is not supported yet for transaction '" + getTransactionInfo() + "', only "
          + DummyXaResource.class + " can participate."));
    }
    xaResources.add(xaRes);
    return true;
  }

  @Override
  public int getStatus() {
    return status;
  }

  @Override
  public void registerSynchronization(Synchronization sync) throws RollbackException {
    if (log.isDebugEnabled()) {
      log.debug("Registering synchronization '{}' for transaction '{}'.", sync, getTransactionInfo());
    }

    if (getStatus() == Status.STATUS_MARKED_ROLLBACK) {
      exceptionThrower.throwException(new RollbackException("Transaction is marked as rollback-only."));
    }
    synchronizations.add(sync);
  }

  public void registerInterposedSynchronization(Synchronization sync) throws RollbackException {
    if (log.isDebugEnabled()) {
      log.debug("Registering interposed synchronization '{}' for transaction '{}'.", sync, getTransactionInfo());
    }
    if (getStatus() == Status.STATUS_MARKED_ROLLBACK) {
      exceptionThrower.throwException(new RollbackException("Transaction is marked as rollback-only."));
    }
    interposedSynchronizations.add(sync);
  }

  @Override
  public void rollback() {
    try {
      if (abandonedTransactionsTracker != null) {
        abandonedTransactionsTracker.abandoned.set(false);
      }
      if (log.isDebugEnabled()) {
        log.debug("Rolling back transaction '{}'.", getTransactionInfo());
      }
      setStatus(Status.STATUS_ROLLING_BACK);

      for (XAResource xaResource : getSortedXaResource(xaResources)) {
        try {
          xaResource.rollback(null);
        } catch (XAException e) {
          log.error("Rollback failed.", e);
        }
      }
      xaResources.clear();
      resources.clear();
      setStatus(Status.STATUS_ROLLEDBACK);
      metricsTemplate.registerTransactionRollback(wasSuspended);
    } catch (Throwable t) {
      metricsTemplate.registerTransactionRollbackFailure(wasSuspended);
      if (t instanceof RuntimeException) {
        exceptionThrower.throwException((RuntimeException) t);
      } else {
        exceptionThrower.throwException(new RuntimeException(t));
      }
    }
  }

  @Override
  public void setRollbackOnly() {
    if (log.isDebugEnabled()) {
      log.debug("Marking transaction '{}' to roll back.", getTransactionInfo());
    }
    setStatus(Status.STATUS_MARKED_ROLLBACK);
  }

  public void setStatus(int status) {
    if (log.isDebugEnabled()) {
      log.debug("Setting transaction '{}' status to '{}'.", getTransactionInfo(), TransactionStatuses.toString(status));
    }
    setStatusInternal(status);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof GafferTransaction)) {
      return false;
    }
    return getGlobalTransactionId().equals(((GafferTransaction) o).getGlobalTransactionId());
  }

  @Override
  public int hashCode() {
    return globalTransactionId.hashCode();
  }

  public boolean beforeCommitValidationRequired() {
    if (beforeCommitValidationRequiredTimeMs < 0) {
      return false;
    }
    return clock.currentTimeMillis() - getStartTimeMillis() > beforeCommitValidationRequiredTimeMs;
  }

  /**
   * Checks if transaction has timed out.
   *
   * <p>Timeout of 0 is considered as no timeout.
   *
   * <p>Timeout of -1 is considered as using default timeout. Gaffer does not currently support default timeouts, so
   * for now it also means no timeout is set.
   */
  public boolean isTimedOut() {
    long timeoutMillis = getTimeoutMillis();
    if (timeoutMillis <= 0) {
      return false;
    }
    return clock.currentTimeMillis() > getStartTimeMillis() + timeoutMillis;
  }

  private void fireBeforeCompletionEvent() {
    for (Synchronization synchronization : synchronizations) {
      synchronization.beforeCompletion();
    }
    for (Synchronization synchronization : interposedSynchronizations) {
      synchronization.beforeCompletion();
    }
  }

  private void fireAfterCompletionEvent() {
    for (Synchronization synchronization : interposedSynchronizations) {
      try {
        synchronization.afterCompletion(getStatus());
      } catch (Throwable t) {
        log.error(t.getMessage(), t);
      }
    }
    for (Synchronization synchronization : synchronizations) {
      try {
        synchronization.afterCompletion(getStatus());
      } catch (Throwable t) {
        log.error(t.getMessage(), t);
      }
    }
  }

  private List<XAResource> getSortedXaResource(List<XAResource> xaResources) {
    List<XAResource> result = new ArrayList<>(xaResources);
    result.sort((a, b) -> {
      int oa = a instanceof OrderedResource ? ((OrderedResource) a).getOrder() : Integer.MAX_VALUE;
      int ob = a instanceof OrderedResource ? ((OrderedResource) b).getOrder() : Integer.MAX_VALUE;
      return Integer.compare(oa, ob);
    });
    return result;
  }

  public String getTransactionInfo() {
    return globalTransactionId + "/" + TransactionStatuses.toString(getStatus());
  }

  private boolean isDoneOrFinishing() {
    return switch (status) {
      case Status.STATUS_PREPARING, Status.STATUS_PREPARED, Status.STATUS_COMMITTING, Status.STATUS_COMMITTED, Status.STATUS_ROLLING_BACK,
           Status.STATUS_ROLLEDBACK -> true;
      default -> false;
    };
  }

  private boolean isWorking() {
    return switch (status) {
      case Status.STATUS_PREPARING, Status.STATUS_PREPARED, Status.STATUS_COMMITTING, Status.STATUS_ROLLING_BACK -> true;
      default -> false;
    };
  }

  private static class AbandonedTransactionsTracker implements Runnable {

    // Abandoned unless something signals otherwise
    private final AtomicBoolean abandoned = new AtomicBoolean(true);
    private final MetricsTemplate metricsTemplate;
    private final Uid globalTransactionId;
    private volatile int status;

    private AbandonedTransactionsTracker(MetricsTemplate metricsTemplate, Uid globalTransactionId, int status) {
      this.metricsTemplate = metricsTemplate;
      this.globalTransactionId = globalTransactionId;
      this.status = status;
    }

    @Override
    public void run() {
      if (abandoned.get()) {
        metricsTemplate.registerTransactionAbandoning();
        log.warn("Transaction '{}' was abandoned.", getTransactionInfo());
      }
    }

    private String getTransactionInfo() {
      return globalTransactionId + "/" + TransactionStatuses.toString(status);
    }

  }

  private static class SingletonCleaner {

    private static final Cleaner INSTANCE = Cleaner.create();
  }
}
