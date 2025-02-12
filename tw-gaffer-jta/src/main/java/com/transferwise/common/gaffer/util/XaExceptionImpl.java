package com.transferwise.common.gaffer.util;

import java.io.Serial;
import javax.transaction.xa.XAException;

public class XaExceptionImpl extends XAException {

  @Serial
  private static final long serialVersionUID = 1L;

  public XaExceptionImpl(int errorCode, Throwable cause) {
    super(errorCode);
    initCause(cause);
  }
}
