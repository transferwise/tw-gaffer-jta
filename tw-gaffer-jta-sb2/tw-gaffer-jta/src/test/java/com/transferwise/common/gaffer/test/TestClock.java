package com.transferwise.common.gaffer.test;

import com.transferwise.common.gaffer.util.Clock;
import java.time.Duration;

public class TestClock implements Clock {

  private long currentTimeMillis = System.currentTimeMillis();

  @Override
  public long currentTimeMillis() {
    return currentTimeMillis;
  }

  public void tick(Duration duration) {
    currentTimeMillis += duration.toMillis();
  }
}
