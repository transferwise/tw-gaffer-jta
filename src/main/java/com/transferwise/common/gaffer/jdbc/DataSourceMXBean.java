package com.transferwise.common.gaffer.jdbc;

import javax.management.MXBean;

@MXBean
public interface DataSourceMXBean {
    public long getAllConnectionGetsCount();

    public long getBufferedConnectionGetsCount();

    public long getNonTransactionalConnectionGetsCount();

    public long getAutoCommitSwitchingCount();
}
