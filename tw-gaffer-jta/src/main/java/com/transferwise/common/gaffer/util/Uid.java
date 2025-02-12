package com.transferwise.common.gaffer.util;

public interface Uid {

  long getStartTimeMillis();

  String getInstanceId();

  byte[] asBytes();
}
