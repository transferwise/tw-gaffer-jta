package com.transferwise.common.gaffer.util;

import jakarta.transaction.Status;

public class TransactionStatuses {

  public static String toString(int status) {
    return switch (status) {
      case Status.STATUS_ACTIVE -> "ACTIVE";
      case Status.STATUS_COMMITTED -> "COMMITED";
      case Status.STATUS_COMMITTING -> "COMMITTING";
      case Status.STATUS_MARKED_ROLLBACK -> "MARKED_ROLLBACK";
      case Status.STATUS_NO_TRANSACTION -> "NO_TRANSACTION";
      case Status.STATUS_PREPARED -> "PREPARED";
      case Status.STATUS_PREPARING -> "PREPARING";
      case Status.STATUS_ROLLEDBACK -> "ROLLEDBACK";
      case Status.STATUS_ROLLING_BACK -> "ROLLING_BACK";
      default -> "UNKNOWN";
    };
  }
}
