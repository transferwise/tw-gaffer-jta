package com.transferwise.common.gaffer;

import io.micrometer.core.instrument.Tag;

public interface MetricsTemplate {

  void init();

  void registerTransactionBeginning();

  void registerTransactionSuspending();

  void registerTransactionResuming();

  void registerTransactionAbandoning();

  void registerTransactionAbandoningTracking();

  void registerTransactionCommit(boolean wasSuspended);

  void registerTransactionRollback(boolean wasSuspended);

  void registerTransactionRollbackFailure(boolean wasSuspended);

  void registerHeuristicTransactionCommit();

  void registerConnectionGet(Tag dataSourceName, boolean transactional);

  void registerConnectionReuse(Tag dataSourceName, boolean transactional);

  void registerConnectionClose(Tag dataSourceName, boolean transactional);

  void registerAutoCommitSwitch(Tag dataSourceName, boolean atAcquire, boolean transactional, boolean setAutoCommit);

  Tag createDataSourceNameTag(String dataSourceName);
}
