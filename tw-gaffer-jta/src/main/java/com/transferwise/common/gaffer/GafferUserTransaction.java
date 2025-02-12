package com.transferwise.common.gaffer;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;
import java.io.Serial;
import java.io.Serializable;

public class GafferUserTransaction implements UserTransaction, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final transient GafferTransactionManager transactionManager;

  public GafferUserTransaction(GafferTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  @Override
  public void begin() throws NotSupportedException, SystemException {
    transactionManager.begin();
  }

  @Override
  public void commit()
      throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
    transactionManager.commit();
  }

  @Override
  public void rollback() throws IllegalStateException, SecurityException, SystemException {
    transactionManager.rollback();
  }

  @Override
  public void setRollbackOnly() throws IllegalStateException, SystemException {
    transactionManager.setRollbackOnly();
  }

  @Override
  public int getStatus() throws SystemException {
    return transactionManager.getStatus();
  }

  @Override
  public void setTransactionTimeout(int seconds) throws SystemException {
    transactionManager.setTransactionTimeout(seconds);
  }

}
