package com.transferwise.common.gaffer.util;

import jakarta.transaction.RollbackException;

public class RollbackExceptionImpl extends RollbackException {

  private static final long serialVersionUID = 1L;

  public RollbackExceptionImpl(String message) {
    super(message);
  }

  public RollbackExceptionImpl(String message, Throwable cause) {
    super(message);
    initCause(cause);
  }
}
