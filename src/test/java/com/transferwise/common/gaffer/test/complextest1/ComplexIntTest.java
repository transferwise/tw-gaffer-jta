package com.transferwise.common.gaffer.test.complextest1;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.transferwise.common.gaffer.jdbc.DataSourceImpl;
import com.transferwise.common.gaffer.test.complextest1.app.ClientsService;
import com.transferwise.common.gaffer.test.complextest1.app.Config;
import com.transferwise.common.gaffer.test.complextest1.app.DatabasesManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

class ComplexIntTest {

  protected static final Logger log = LoggerFactory.getLogger(ComplexIntTest.class);

  protected static final String CONFIG_KEY_SECRETS_DATABASE_URL = "database.secrets.url";
  protected static final String CONFIG_KEY_MAX_POOL_SIZE = "maxPoolSize";
  protected static final String CLIENT_NAME = "OY Bemmimehed";
  protected static final String TABLE_ACCOUNTS = "accounts.accounts";
  protected static final String TABLE_PASSWORDS = "secrets.passwords";
  protected static final String TABLE_USERS = "users.users";
  protected static final String TABLE_CLIENTS = "clients.clients";

  protected ClassPathXmlApplicationContext appCtxt;

  @BeforeEach
  public void setUpProperties() {
    System.setProperty(CONFIG_KEY_SECRETS_DATABASE_URL, "jdbc:hsqldb:mem:secrets");
    System.setProperty(CONFIG_KEY_MAX_POOL_SIZE, "1");
  }

  protected void setUp() {
    appCtxt = new ClassPathXmlApplicationContext("/com/transferwise/common/gaffer/test/complextest1/app/applicationContext.xml");
  }

  @AfterEach
  public void tearDown() {
    appCtxt.close();
  }

  @Test
  void testFullCommit() {
    setUp();
    Stat stat = new Stat("testFullCommit");
    getClientsService().createClient(CLIENT_NAME);
    log.info("{}", stat);

    assertThat(getTableCount(TABLE_CLIENTS), equalTo(1));
    assertThat(getTableCount(TABLE_USERS), equalTo(1));
    assertThat(getTableCount(TABLE_PASSWORDS), equalTo(1));
    assertThat(getTableCount(TABLE_ACCOUNTS), equalTo(1));
  }

  @Test
  void testFullRollback() {
    setUp();
    try {
      getConfig().setFailPasswordCreation(true);
      Stat stat = new Stat("testFullRollback");
      try {
        getClientsService().createClient(CLIENT_NAME);
      } finally {
        log.info("{}", stat);
      }
      throw new IllegalStateException("Test has invalid logic, recheck it!");
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      log.debug(e.getMessage(), e);
    }

    assertThat(getTableCount(TABLE_CLIENTS), equalTo(0));
    assertThat(getTableCount(TABLE_USERS), equalTo(0));
    assertThat(getTableCount(TABLE_PASSWORDS), equalTo(0));
    assertThat(getTableCount(TABLE_ACCOUNTS), equalTo(0));
  }

  /**
   * Use incorrect login for secrets database. When getConnection() is not called, there would not be an exception and vice versa.
   */
  @Test
  void testEfficientResourceHandlingTestingMethod() {
    Assertions.assertThrows(CannotGetJdbcConnectionException.class, () -> {
      System.setProperty(CONFIG_KEY_SECRETS_DATABASE_URL, "Invalid URL.");
      setUp();
      Stat stat = new Stat("testEfficientResourceHandlingTestingMethod");
      try {
        getClientsService().createClient(CLIENT_NAME);
      } finally {
        log.info("{}", stat);
      }
    });
  }

  /**
   * Secrets database is not used, so its getConnection() should never called.
   */
  @Test
  void testEfficientResourceHandling() {
    System.setProperty(CONFIG_KEY_SECRETS_DATABASE_URL, "Invalid URL.");
    setUp();
    getConfig().setCreatePassword(false);
    Stat stat = new Stat("testEfficientResourceHandling");
    getClientsService().createClient(CLIENT_NAME);
    log.info("{}", stat);

    assertThat(getTableCount(TABLE_CLIENTS), equalTo(1));
    assertThat(getTableCount(TABLE_USERS), equalTo(1));
    assertThat(getTableCount(TABLE_PASSWORDS), equalTo(0));
    assertThat(getTableCount(TABLE_ACCOUNTS), equalTo(1));
  }

  @Test
  void testParallelWork() {
    final int threadsCount = 10;
    final int iterations = 10000;
    System.setProperty(CONFIG_KEY_MAX_POOL_SIZE, String.valueOf(Math.max(1, threadsCount / 2)));
    setUp();
    final Stat stat = new Stat("testParallelWork");
    ExecutorService executor = Executors.newFixedThreadPool(threadsCount);

    for (int i = 0; i < iterations; i++) {
      executor.execute(() -> getClientsService().createClient(CLIENT_NAME));
    }
    executor.shutdown();
    try {
      executor.awaitTermination(1, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      log.trace(e.getMessage(), e);
    }
    log.info("{}", stat);

    assertThat(getTableCount(TABLE_CLIENTS), equalTo(iterations));
    assertThat(getTableCount(TABLE_USERS), equalTo(iterations));
    assertThat(getTableCount(TABLE_PASSWORDS), equalTo(iterations));
    assertThat(getTableCount(TABLE_ACCOUNTS), equalTo(iterations));
  }

  /**
   * Test if the setup can work with non transactional code.
   */
  @Test
  void testLocalTransactionsFullCommit() {
    setUp();
    Stat stat = new Stat("testLocalTransactionsFullCommit");
    getClientsService().createClientLegacy(CLIENT_NAME);
    log.info("{}", stat);

    assertThat(getTableCount(TABLE_CLIENTS), equalTo(1));
    assertThat(getTableCount(TABLE_USERS), equalTo(1));
    assertThat(getTableCount(TABLE_PASSWORDS), equalTo(1));
    assertThat(getTableCount(TABLE_ACCOUNTS), equalTo(1));
  }

  /**
   * Test if the setup can work with non transactional code.
   */
  @Test
  void testLocalTransactionsFullRollback() {
    setUp();
    try {
      getConfig().setFailPasswordCreation(true);
      Stat stat = new Stat("testLocalTransactionsFullRollback");
      try {
        getClientsService().createClientLegacy(CLIENT_NAME);
      } finally {
        log.info("{}", stat);
      }
      throw new IllegalStateException("Programming error detected. Yell at developers immediately!");
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      log.debug(e.getMessage(), e);
    }

    // Local transaction
    assertThat(getTableCount(TABLE_CLIENTS), equalTo(1));
    // XA transaction #1
    assertThat(getTableCount(TABLE_USERS), equalTo(0));
    // XA transaction #1
    assertThat(getTableCount(TABLE_PASSWORDS), equalTo(0));
    // XA transaction #2, committed before #1 rolls back
    assertThat(getTableCount(TABLE_ACCOUNTS), equalTo(1));
  }

  @Test
  void testFullCommitWithHibernate() {
    setUp();
    getConfig().setUseHibernate(true);
    Stat stat = new Stat("testFullCommit");
    getClientsService().createClient(CLIENT_NAME);
    log.info("{}", stat);

    assertThat(getTableCount(TABLE_CLIENTS), equalTo(1));
    assertThat(getTableCount(TABLE_USERS), equalTo(1));
    assertThat(getTableCount(TABLE_PASSWORDS), equalTo(1));
    assertThat(getTableCount(TABLE_ACCOUNTS), equalTo(1));
  }

  @Test
  public void testConnReleasedAfterTxRollback() {
    setUp();
    try {
      getConfig().setFailPasswordCreation(true);
      Stat stat = new Stat("testConnReleasedAfterTxRollback");
      try {
        getClientsService().deleteClient(CLIENT_NAME);
      } finally {
        log.info("{}", stat);
      }
      throw new IllegalStateException("Test has invalid logic, recheck it!");
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      log.debug(e.getMessage(), e);
    }

    assertThat(getAccountDataSource().getAllConnectionGetsCount(), equalTo(1L));
    assertThat(getAccountDataSource().getClosedConnectionsCount(), equalTo(1L));
  }

  protected int getTableCount(String tableName) {
    return appCtxt.getBean(DatabasesManager.class).getTableRowsCount(tableName);
  }

  protected Config getConfig() {
    return (Config) appCtxt.getBean("config");
  }

  protected ClientsService getClientsService() {
    return (ClientsService) appCtxt.getBean("clientsService");
  }

  DataSourceImpl getAccountDataSource() {
    return ((DataSourceImpl) appCtxt.getBean("accountsDataSource"));
  }

  protected static class Stat {

    private final long start = System.currentTimeMillis();
    private final String action;

    protected Stat(String action) {
      this.action = action;
    }

    @Override
    public String toString() {
      return action + " - " + (System.currentTimeMillis() - start) + " ms.";
    }
  }
}
