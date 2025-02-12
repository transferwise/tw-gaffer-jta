package com.transferwise.common.gaffer;

import com.transferwise.common.gaffer.util.Clock;
import com.transferwise.common.gaffer.util.ExceptionThrower;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionSynchronizationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableObject;

@Slf4j
public class DefaultGafferTransactionManager implements GafferTransactionManager {

  private final ThreadLocal<MutableObject<GafferTransaction>> transactions = new ThreadLocal<>();
  private final ThreadLocal<MutableObject<Integer>> transactionTimeoutsSeconds = new ThreadLocal<>();

  private final MetricsTemplate metricsTemplate;
  private final ExceptionThrower exceptionThrower;
  private final Clock clock;

  private final GafferJtaProperties gafferJtaProperties;
  private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  public DefaultGafferTransactionManager(GafferJtaProperties gafferJtaProperties, MetricsTemplate metricsTemplate, Clock clock) {
    this.metricsTemplate = metricsTemplate;
    this.clock = clock;
    exceptionThrower = new ExceptionThrower(gafferJtaProperties.isLogExceptions());
    this.gafferJtaProperties = gafferJtaProperties;
    transactionSynchronizationRegistry = new GafferTransactionSynchronizationRegistry(this, gafferJtaProperties);
  }

  @Override
  public void begin() throws NotSupportedException {
    Transaction transaction = getTransaction();
    if (transaction != null) {
      exceptionThrower.throwException(new NotSupportedException("Nested transactions are not supported."));
    }

    GafferTransaction gafferTransaction = new GafferTransaction(clock, gafferJtaProperties, metricsTemplate);
    gafferTransaction.begin(getTransactionTimeout(), gafferJtaProperties.getBeforeCommitValidationRequiredTime().toMillis());
    setTransaction(gafferTransaction);

    metricsTemplate.registerTransactionBeginning();
  }

  @Override
  public void commit() throws RollbackException, SecurityException, IllegalStateException {
    GafferTransaction transaction = getTransactionImpl();
    if (transaction == null) {
      exceptionThrower.throwException(new IllegalStateException("Can not commit. Current thread is not associated with transaction."));
    } else {
      try {
        transaction.commit();
      } finally {
        transactions.remove();
      }
    }
  }

  @Override
  public int getStatus() {
    GafferTransaction transaction = getTransactionImpl();
    return transaction == null ? Status.STATUS_NO_TRANSACTION : transaction.getStatus();
  }

  @Override
  public Transaction getTransaction() {
    var holder = transactions.get();
    return holder == null ? null : holder.getValue();
  }

  protected void setTransaction(GafferTransaction transaction) {
    var holder = transactions.get();
    if (holder == null) {
      holder = new MutableObject<>();
      transactions.set(holder);
    }
    holder.setValue(transaction);
  }

  @Override
  public MetricsTemplate getMetricsTemplate() {
    return metricsTemplate;
  }

  public GafferTransaction getTransactionImpl() {
    return (GafferTransaction) getTransaction();
  }

  @Override
  public void resume(Transaction transaction) throws IllegalStateException {
    Transaction currentTransaction = getTransaction();
    if (currentTransaction != null) {
      throw new IllegalStateException("Can not resume. Current thread is already associated with transaction.");
    }
    if (!(transaction instanceof GafferTransaction)) {
      throw new IllegalStateException("Can not resume. Unsupported transaction object '" + transaction + "' provided.");
    }
    GafferTransaction gafferTransaction = (GafferTransaction) transaction;
    if (log.isDebugEnabled()) {
      log.debug("Resuming transaction '{}'.", gafferTransaction.getTransactionInfo());
    }
    setTransaction(gafferTransaction);
    gafferTransaction.setSuspended(false);

    metricsTemplate.registerTransactionResuming();
  }

  @Override
  public void rollback() throws IllegalStateException, SecurityException {
    GafferTransaction transaction = getTransactionImpl();
    if (transaction == null) {
      exceptionThrower.throwException(new IllegalStateException("Can not rollback. Current thread is not associated with transaction."));
    } else {
      try {
        transaction.rollback();
      } finally {
        transactions.remove();
      }
    }
  }

  @Override
  public void setRollbackOnly() throws IllegalStateException, SystemException {
    Transaction transaction = getTransaction();
    if (transaction == null) {
      exceptionThrower.throwException(new IllegalStateException("Can not mark to rollback. Current thread is not associated with transaction."));
    } else {
      transaction.setRollbackOnly();
    }
  }

  @Override
  public void setTransactionTimeout(int seconds) {
    var holder = transactionTimeoutsSeconds.get();
    if (holder == null) {
      holder = new MutableObject<>();
      transactionTimeoutsSeconds.set(holder);
    }
    holder.setValue(seconds);
  }

  public Integer getTransactionTimeout() {
    var holder = transactionTimeoutsSeconds.get();
    return holder == null ? null : holder.getValue();
  }

  @Override
  public Transaction suspend() {
    GafferTransaction transaction = getTransactionImpl();

    if (transaction != null) {
      if (log.isDebugEnabled()) {
        log.debug("Suspending transaction '" + transaction.getTransactionInfo() + "'.");
      }
      transaction.setSuspended(true);
      transactions.remove();
      metricsTemplate.registerTransactionSuspending();
    } else {
      log.debug("Suspend called for non-existent transaction.");
    }
    return transaction;
  }

  @Override
  public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
    return transactionSynchronizationRegistry;
  }
}
