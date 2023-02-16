package com.transferwise.common.gaffer.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.transferwise.common.gaffer.jdbc.GafferJtaDataSource;
import com.transferwise.common.gaffer.starter.test.TestApplication;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class)
class AutoConfigurationIntTest {

  @Autowired
  private DataSource dataSource;

  @Test
  void testIfDataSourceIsCorrectlyWrapped() {
    var springAdapter = (GafferJtaSpringIntegrationDataSourceAdapter) dataSource;
    var gafferDataSource = (GafferJtaDataSource) springAdapter.getTargetDataSource();

    assertNotNull(gafferDataSource);
    assertNotNull(gafferDataSource.getValidationTimeoutSeconds());

    assertEquals(31, gafferDataSource.getValidationTimeoutSeconds());
    assertEquals(15, gafferDataSource.getOrder());

    assertEquals(gafferDataSource.getParentDataSource(), springAdapter);

    var hikariDataSource = gafferDataSource.getTargetDataSource();

    assertNotNull(hikariDataSource);
  }
}
