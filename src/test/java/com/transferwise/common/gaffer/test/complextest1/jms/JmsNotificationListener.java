package com.transferwise.common.gaffer.test.complextest1.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsNotificationListener {

  private static final Logger log = LoggerFactory.getLogger(JmsNotificationListener.class);

  public void handleMessage(String message) {
    log.info("Text message received." + message);
  }
}
