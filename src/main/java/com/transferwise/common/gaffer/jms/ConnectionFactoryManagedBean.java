package com.transferwise.common.gaffer.jms;

import javax.management.MXBean;

@MXBean
public interface ConnectionFactoryManagedBean {

  long getAllSessionGetsCount();

  long getBufferedSessionGetsCount();

  long getNonTransactionalSessionGetsCount();
}
