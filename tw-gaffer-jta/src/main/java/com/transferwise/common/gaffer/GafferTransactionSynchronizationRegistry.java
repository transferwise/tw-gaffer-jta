package com.transferwise.common.gaffer;

import com.transferwise.common.gaffer.util.ExceptionThrower;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

public class GafferTransactionSynchronizationRegistry implements TransactionSynchronizationRegistry {

  private final DefaultGafferTransactionManager transactionManager;
  private final ExceptionThrower exceptionThrower;

  public GafferTransactionSynchronizationRegistry(DefaultGafferTransactionManager transactionManager, GafferJtaProperties gafferJtaProperties) {
    this.transactionManager = transactionManager;
    exceptionThrower = new ExceptionThrower(gafferJtaProperties.isLogExceptions());
  }

  @Override
  public Object getTransactionKey() {
    GafferTransaction transaction = getTransaction();
    return transaction == null ? null : transaction.getGlobalTransactionId();
  }

  @Override
  public void putResource(Object key, Object value) {
    GafferTransaction transaction = getTransaction();
    if (transaction == null) {
      exceptionThrower.throwException(new IllegalStateException("Current thread is not associated with transaction."));
    } else {
      transaction.putResource(key, value);
    }
  }

  @Override
  public Object getResource(Object key) {
    GafferTransaction transaction = getTransaction();
    if (transaction == null) {
      exceptionThrower.throwException(new IllegalStateException("Current thread is not associated with transaction."));
      return null;
    }
    return transaction.getResource(key);
  }

  @Override
  public void registerInterposedSynchronization(Synchronization sync) {
    GafferTransaction transaction = getTransaction();
    if (transaction == null) {
      exceptionThrower.throwException(new IllegalStateException("Current thread is not associated with transaction."));
    } else {
      try {
        transaction.registerInterposedSynchronization(sync);
      } catch (RollbackException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public int getTransactionStatus() {
    return transactionManager.getStatus();
  }

  @Override
  public void setRollbackOnly() {
    GafferTransaction transaction = getTransaction();
    if (transaction == null) {
      exceptionThrower.throwException(new IllegalStateException("Current thread is not associated with transaction."));
    } else {
      transaction.setRollbackOnly();
    }
  }

  @Override
  public boolean getRollbackOnly() {
    GafferTransaction transaction = getTransaction();
    if (transaction == null) {
      exceptionThrower.throwException(new IllegalStateException("Current thread is not associated with transaction."));
      return false;
    }
    return transaction.getStatus() == Status.STATUS_MARKED_ROLLBACK;
  }

  private GafferTransaction getTransaction() {
    return transactionManager.getTransactionImpl();
  }

}
