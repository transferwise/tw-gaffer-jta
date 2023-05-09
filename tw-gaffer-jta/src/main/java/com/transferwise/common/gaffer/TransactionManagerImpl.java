package com.transferwise.common.gaffer;

import com.transferwise.common.gaffer.util.ExceptionThrower;
import com.transferwise.common.gaffer.util.FormatLogger;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

public class TransactionManagerImpl implements TransactionManager {

  private static final FormatLogger log = new FormatLogger(TransactionManagerImpl.class);

  private final ThreadLocal<TransactionImpl> transactions = new ThreadLocal<>();
  private final ThreadLocal<Integer> transactionTimeoutsSeconds = new ThreadLocal<>();

  private final TransactionManagerStatistics statistics;
  private final ExceptionThrower exceptionThrower;

  private final Configuration configuration;

  public TransactionManagerImpl(TransactionManagerStatistics statistics, Configuration configuration) {
    this.statistics = statistics;
    exceptionThrower = new ExceptionThrower(configuration.isLogExceptions());
    this.configuration = configuration;
  }

  @Override
  public void begin() throws NotSupportedException {
    Transaction transaction = getTransaction();
    if (transaction != null) {
      exceptionThrower.throwException(new NotSupportedException("Nested transactions are not supported."));
    }

    TransactionImpl transactionImpl = new TransactionImpl();
    transactionImpl.begin(transactionTimeoutsSeconds.get(), configuration.getBeforeCommitValidationRequiredTimeMs());
    transactions.set(transactionImpl);

    statistics.markBegin();
  }

  @Override
  public void commit() throws RollbackException, SecurityException, IllegalStateException {
    TransactionImpl transaction = getTransactionImpl();
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
    TransactionImpl transaction = getTransactionImpl();
    return transaction == null ? Status.STATUS_NO_TRANSACTION : transaction.getStatus();
  }

  @Override
  public Transaction getTransaction() {
    return transactions.get();
  }

  public TransactionImpl getTransactionImpl() {
    return (TransactionImpl) getTransaction();
  }

  @Override
  public void resume(Transaction transaction) throws IllegalStateException {
    Transaction currentTransaction = getTransaction();
    if (currentTransaction != null) {
      throw new IllegalStateException("Can not resume. Current thread is already associated with transaction.");
    }
    if (!(transaction instanceof TransactionImpl)) {
      throw new IllegalStateException("Can not resume. Unsupported transaction object '" + transaction + "' provided.");
    }
    TransactionImpl transactionImpl = (TransactionImpl) transaction;
    if (log.isDebugEnabled()) {
      log.debug("Resuming transaction '%s'.", transactionImpl.getTransactionInfo());
    }
    transactions.set(transactionImpl);
    transactionImpl.setSuspended(false);

    statistics.markResume();
  }

  @Override
  public void rollback() throws IllegalStateException, SecurityException {
    TransactionImpl transaction = getTransactionImpl();
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
    transactionTimeoutsSeconds.set(seconds);
  }

  public Integer getTransactionTimeout() {
    return transactionTimeoutsSeconds.get();
  }

  @Override
  public Transaction suspend() {
    TransactionImpl transaction = getTransactionImpl();

    if (transaction != null) {
      if (log.isDebugEnabled()) {
        log.debug("Suspending transaction '" + transaction.getTransactionInfo() + "'.");
      }
      transaction.setSuspended(true);
      transactions.remove();
      statistics.markSuspend();
    } else {
      log.debug("Suspend called for non-existent transaction.");
    }
    return transaction;
  }

  public TransactionManagerStatistics getTransactionManagerStatistics() {
    return statistics;
  }
}
