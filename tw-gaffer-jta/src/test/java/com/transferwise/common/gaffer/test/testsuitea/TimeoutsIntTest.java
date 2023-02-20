package com.transferwise.common.gaffer.test.testsuitea;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.transferwise.common.gaffer.ServiceRegistryHolder;
import com.transferwise.common.gaffer.TestUtils;
import com.transferwise.common.gaffer.TransactionImpl;
import com.transferwise.common.gaffer.test.TestClock;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

@BaseTestEnvironment
@TestInstance(Lifecycle.PER_CLASS)
class TimeoutsIntTest {

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private TestClock testClock;

  @BeforeEach
  void beforeEach() {
    getSelf().testClock = testClock;
    TestUtils.setClock(testClock);
  }

  @Test
  void testThatTimeoutsAreCleared() {
    getSelf().doSomethingWithTimeout();
    getSelf().doSomethingWithoutTimeout();

    getSelf().doSomethingWithTimeout();
    getSelf().doSomethingWithoutTimeout();
  }

  @Transactional(timeout = 1)
  public void doSomethingWithTimeout() {
    assertThat(ServiceRegistryHolder.getServiceRegistry().getTransactionManager().getTransactionTimeout(), equalTo(1));
  }

  @Transactional
  public void doSomethingWithoutTimeout() {
    var transactionManager = ServiceRegistryHolder.getServiceRegistry().getTransactionManager();
    assertThat(transactionManager.getTransactionTimeout(), equalTo(0));

    var transaction = (TransactionImpl) transactionManager.getTransaction();
    testClock.tick(Duration.ofMillis(2));

    // Timeout of 0 means there is no timeout.
    assertThat(transaction.isTimedOut(), equalTo(false));
  }

  protected TimeoutsIntTest getSelf() {
    return applicationContext.getBean(TimeoutsIntTest.class);
  }

}
