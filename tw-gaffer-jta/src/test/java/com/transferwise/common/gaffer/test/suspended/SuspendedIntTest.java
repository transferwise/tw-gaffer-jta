package com.transferwise.common.gaffer.test.suspended;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.transferwise.common.gaffer.test.BaseExtension;
import com.transferwise.common.gaffer.test.MetricsTestHelper;
import com.transferwise.common.gaffer.test.suspended.app.ClientsService;
import com.transferwise.common.gaffer.test.suspended.app.DatabasesManager;
import com.transferwise.common.gaffer.test.suspended.app.TestApplication;
import jakarta.annotation.Resource;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.UnexpectedRollbackException;

@Slf4j
@ActiveProfiles(profiles = {"integration"})
@ExtendWith(BaseExtension.class)
@SpringBootTest(classes = {TestApplication.class})
@TestInstance(Lifecycle.PER_CLASS)
class SuspendedIntTest {

  @Resource(name = "clientsService")
  private ClientsService clientsService;

  @Resource(name = "databasesManager")
  private DatabasesManager databasesManager;

  @Resource(name = "clientsInnerDataSource")
  private DataSource clientsInnerDataSource;

  @Resource(name = "logsInnerDataSource")
  private DataSource logsInnerDataSource;

  @Autowired
  private MetricsTestHelper mth;

  @BeforeEach
  public void beforeEach() {
    databasesManager.deleteRows();
    mth.cleanup();
  }

  @Test
  void testSuccess2() {
    clientsService.createClient2("Aadus");

    log.info("Client service invocation finished.");
    assertThat(databasesManager.getTableRowsCount("clients.clients"), equalTo(1));
    assertThat(databasesManager.getTableRowsCount("logs.logs"), equalTo(1));
    assertThat(((org.apache.tomcat.jdbc.pool.DataSource) clientsInnerDataSource).getNumActive(), equalTo(0));
    assertThat(((org.apache.tomcat.jdbc.pool.DataSource) logsInnerDataSource).getNumActive(), equalTo(0));

    assertThat(mth.getCount("gaffer.connection.get", "[tag(dataSource=logs),tag(transactional=true)]"), equalTo(1d));
    assertThat(mth.getCount("gaffer.connection.get", "[tag(dataSource=clients),tag(transactional=true)]"), equalTo(1d));

    // Queries for test assertions
    assertThat(mth.getCount("gaffer.connection.get", "tags=[tag(dataSource=logs),tag(transactional=false)]"), equalTo(1d));
    assertThat(mth.getCount("gaffer.connection.get", "tags=[tag(dataSource=clients),tag(transactional=false)]"), equalTo(1d));

    assertThat(mth.getCount("gaffer.connection.close", "[tag(dataSource=logs),tag(transactional=true)]"), equalTo(1d));
    assertThat(mth.getCount("gaffer.connection.close", "[tag(dataSource=clients),tag(transactional=true)]"), equalTo(1d));

    // Queries for test assertions
    assertThat(mth.getCount("gaffer.connection.close", "tags=[tag(dataSource=logs),tag(transactional=false)]"), equalTo(1d));
    assertThat(mth.getCount("gaffer.connection.close", "tags=[tag(dataSource=clients),tag(transactional=false)]"), equalTo(1d));

    assertThat(mth.getCount("gaffer.transaction.resume", "tags=[]"), equalTo(1d));
    assertThat(mth.getCount("gaffer.transaction.suspend", "tags=[]"), equalTo(1d));

    assertThat(mth.getCount("gaffer.connection.autocommit.switch",
        "tags=[tag(atAcquire=true),tag(autoCommit=false),tag(dataSource=clients),tag(transactional=true)]"), equalTo(1d));
    assertThat(mth.getCount("gaffer.connection.autocommit.switch",
        "tags=[tag(atAcquire=true),tag(autoCommit=false),tag(dataSource=logs),tag(transactional=true)]"), equalTo(1d));

    assertThat(mth.getCount("gaffer.transaction.begin", "tags=[]"), equalTo(2d));
    assertThat(mth.getCount("gaffer.transaction.commit", "tags=[tag(suspended=false)]"), equalTo(1d));
    assertThat(mth.getCount("gaffer.transaction.commit", "tags=[tag(suspended=true)]"), equalTo(1d));

    assertThat(mth.getGaugeValue("gaffer.transactions.suspended", -1d, ""), equalTo(0d));
    assertThat(mth.getGaugeValue("gaffer.transactions.active", -1d, ""), equalTo(0d));
  }

  @Test
  void testSuccess() {
    clientsService.createClient("Aadu");
    assertThat(databasesManager.getTableRowsCount("clients.clients"), equalTo(1));
  }

  @Test
  void testError() {
    boolean wasRollback = false;
    try {
      clientsService.createClient("Invalid name");
    } catch (UnexpectedRollbackException e) {
      wasRollback = true;
    }
    assertThat(wasRollback, equalTo(true));
    assertThat(databasesManager.getTableRowsCount("clients.clients"), equalTo(0));
    assertThat(databasesManager.getTableRowsCount("logs.logs"), equalTo(1));

    assertThat(mth.getCount("gaffer.connection.get", "[tag(dataSource=logs),tag(transactional=true)]"), equalTo(1d));
    assertThat(mth.getCount("gaffer.connection.get", "[tag(dataSource=clients),tag(transactional=true)]"), equalTo(0d));

    assertThat(mth.getCount("gaffer.connection.close", "[tag(dataSource=logs),tag(transactional=true)]"), equalTo(1d));

    assertThat(mth.getCount("gaffer.transaction.resume", "tags=[]"), equalTo(1d));
    assertThat(mth.getCount("gaffer.transaction.suspend", "tags=[]"), equalTo(1d));

    assertThat(mth.getCount("gaffer.connection.autocommit.switch",
        "tags=[tag(atAcquire=true),tag(autoCommit=false),tag(dataSource=logs),tag(transactional=true)]"), equalTo(1d));

    assertThat(mth.getCount("gaffer.transaction.begin", "tags=[]"), equalTo(2d));

    // Empty commit
    assertThat(mth.getCount("gaffer.transaction.commit", "tags=[tag(suspended=false)]"), equalTo(1d));

    assertThat(mth.getCount("gaffer.transaction.rollback", "tags=[tag(suspended=true)]"), equalTo(1d));
  }
}
