package com.transferwise.common.gaffer;

import com.transferwise.common.gaffer.util.Clock;

public class TestUtils {

  public static void setClock(Clock clock) {
    ServiceRegistryHolder.getServiceRegistry().setClock(clock);
  }

}
