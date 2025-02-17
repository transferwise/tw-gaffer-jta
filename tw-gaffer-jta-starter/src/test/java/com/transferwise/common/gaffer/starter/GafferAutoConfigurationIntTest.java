package com.transferwise.common.gaffer.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.transferwise.common.baseutils.ExceptionUtils;
import com.transferwise.common.baseutils.transactionsmanagement.ITransactionsHelper;
import com.transferwise.common.gaffer.jdbc.GafferJtaDataSource;
import com.transferwise.common.gaffer.starter.test.TestApplication;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Isolation;

@SpringBootTest(classes = TestApplication.class)
class GafferAutoConfigurationIntTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private ITransactionsHelper transactionsHelper;

  @Test
  void testIfDataSourceIsCorrectlyWrapped() {
    var springAdapter = (GafferJtaSpringIntegrationDataSourceAdapter) dataSource;
    var gafferDataSource = (GafferJtaDataSource) springAdapter.getTargetDataSource();

    assertNotNull(gafferDataSource);

    assertEquals(31, gafferDataSource.getValidationTimeoutSeconds());
    assertEquals(15, gafferDataSource.getCommitOrder());

    assertEquals(gafferDataSource.getParentDataSource(), springAdapter);

    var hikariDataSource = gafferDataSource.getTargetDataSource();

    assertNotNull(hikariDataSource);
  }

  @Test
  void testIfIsolationLevelWorks() {
    transactionsHelper.withTransaction().withIsolation(Isolation.READ_UNCOMMITTED).asReadOnly().run(() ->
        ExceptionUtils.doUnchecked(() -> {
          try (var connection = dataSource.getConnection()) {
            assertEquals(Isolation.READ_UNCOMMITTED.value(), connection.getTransactionIsolation());
            assertTrue(connection.isReadOnly());
          }
        })
    );

    // Hikari will give the same connection we used before with near certainty.
    // The isolation level and read only flag should now go back to default.
    transactionsHelper.withTransaction().run(() ->
        ExceptionUtils.doUnchecked(() -> {
          try (var connection = dataSource.getConnection()) {
            assertEquals(0, connection.getTransactionIsolation());
            assertFalse(connection.isReadOnly());
          }
        })
    );
  }
}
