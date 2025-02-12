package com.transferwise.common.gaffer.util;

import jakarta.transaction.RollbackException;
import java.io.Serial;

public class HeuristicMixedExceptionImpl extends RollbackException {

  @Serial
  private static final long serialVersionUID = 1L;

  public HeuristicMixedExceptionImpl(String message, Throwable cause) {
    super(message);
    initCause(cause);
  }
}
