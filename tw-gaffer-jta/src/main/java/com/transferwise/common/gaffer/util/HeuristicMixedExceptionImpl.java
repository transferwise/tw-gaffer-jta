package com.transferwise.common.gaffer.util;

import jakarta.transaction.RollbackException;

public class HeuristicMixedExceptionImpl extends RollbackException {

  private static final long serialVersionUID = 1L;

  public HeuristicMixedExceptionImpl(String message) {
    super(message);
  }

  public HeuristicMixedExceptionImpl(String message, Throwable cause) {
    super(message);
    initCause(cause);
  }
}
