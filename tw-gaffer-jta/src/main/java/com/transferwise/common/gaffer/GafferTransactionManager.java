package com.transferwise.common.gaffer;

import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

public interface GafferTransactionManager extends TransactionManager {

  TransactionSynchronizationRegistry getTransactionSynchronizationRegistry();

  Transaction getTransaction();

  MetricsTemplate getMetricsTemplate();
}
