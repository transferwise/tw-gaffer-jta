package com.transferwise.common.gaffer.jdbc;

import javax.management.MXBean;

@MXBean
public interface DataSourceMXBean {
    long getAllConnectionGetsCount();

    long getBufferedConnectionGetsCount();

    long getNonTransactionalConnectionGetsCount();

    long getAutoCommitSwitchingCount();
}
