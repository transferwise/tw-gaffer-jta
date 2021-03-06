package com.transferwise.common.gaffer;

import javax.management.MXBean;

@MXBean
public interface TransactionManagerStatisticsManagedBean {

  long getStartedTransactionsCount();

  long getCommittedTransactionsCount();

  long getRolledBackTransactionsCount();

  long getActiveTransactionsCount();

  long getSuspendedTransactionsCount();

  long getAbandonedTransactionsCount();

  long getFailedRollbacksCount();

  long getHeuristicCommitsCount();
}
