package com.transferwise.common.gaffer.util;

public interface Uid {
    long getStartTimeMillis();

    int getSequence();

    String getInstanceId();

    byte[] asBytes();
}
