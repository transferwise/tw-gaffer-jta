package com.transferwise.common.gaffer.jdbc;

import javax.management.MXBean;

@MXBean
public interface DataSourceManagedBean {

  long getAllConnectionGetsCount();

  long getBufferedConnectionGetsCount();

  long getNonTransactionalConnectionGetsCount();

  long getAutoCommitSwitchingCount();
}
