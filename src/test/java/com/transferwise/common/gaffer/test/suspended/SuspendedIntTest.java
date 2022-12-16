package com.transferwise.common.gaffer.test.suspended;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.transferwise.common.gaffer.test.suspended.app.ClientsService;
import com.transferwise.common.gaffer.test.suspended.app.DatabasesManager;
import com.transferwise.common.gaffer.util.FormatLogger;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.UnexpectedRollbackException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:/com/transferwise/common/gaffer/test/suspended/applicationContext.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SuspendedIntTest {

  private static final FormatLogger log = new FormatLogger(SuspendedIntTest.class);
  @Resource(name = "clientsService")
  private ClientsService clientsService;

  @Resource(name = "databasesManager")
  private DatabasesManager databasesManager;

  @Resource(name = "clientsInnerDataSource")
  private DataSource clientsInnerDataSource;

  @Resource(name = "logsInnerDataSource")
  private DataSource logsInnerDataSource;

  @BeforeEach
  public void afterTest() {
    databasesManager.deleteRows();
  }

  @Test
  void testSuccess2() {
    clientsService.createClient2("Aadus");

    log.info("Client service invocation finished.");
    assertThat(databasesManager.getTableRowsCount("clients.clients"), equalTo(1));
    assertThat(databasesManager.getTableRowsCount("logs.logs"), equalTo(1));
    assertThat(((org.apache.tomcat.jdbc.pool.DataSource) clientsInnerDataSource).getNumActive(), equalTo(0));
    assertThat(((org.apache.tomcat.jdbc.pool.DataSource) logsInnerDataSource).getNumActive(), equalTo(0));
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
  }
}
