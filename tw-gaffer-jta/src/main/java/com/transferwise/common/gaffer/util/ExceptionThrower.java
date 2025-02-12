package com.transferwise.common.gaffer.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExceptionThrower {

  private final boolean logExceptions;

  public ExceptionThrower(boolean logExceptions) {
    this.logExceptions = logExceptions;
  }

  public <T extends Exception> void throwException(T e) throws T {
    if (logExceptions) {
      log.error(e.getMessage(), e);
    }
    throw e;
  }
}
