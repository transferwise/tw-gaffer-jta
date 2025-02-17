package com.transferwise.common.gaffer.test.testsuitea;

import com.transferwise.common.gaffer.GafferTransactionManager;
import com.transferwise.common.gaffer.jdbc.GafferJtaDataSource;
import com.transferwise.common.gaffer.test.BaseTestApplication;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class TestApplication extends BaseTestApplication {

  @Bean
  public DataSource usersDataSource(GafferTransactionManager gafferTransactionManager) {
    var hds = new HikariDataSource();
    hds.setJdbcUrl("jdbc:hsqldb:mem:users");
    hds.setUsername("SA");
    hds.setPassword("");
    hds.setPoolName("users");

    return new GafferJtaDataSource(gafferTransactionManager, hds.getPoolName(), hds);
  }

  @Bean
  public TimeoutsIntTest timeoutsTest() {
    return new TimeoutsIntTest();
  }

}
