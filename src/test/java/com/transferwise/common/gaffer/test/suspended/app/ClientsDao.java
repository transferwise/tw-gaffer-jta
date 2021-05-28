package com.transferwise.common.gaffer.test.suspended.app;

import com.transferwise.common.gaffer.util.FormatLogger;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository("clientsDAO")
public class ClientsDao {

  private static final FormatLogger log = new FormatLogger(ClientsDao.class);
  private JdbcTemplate jdbcTemplate;

  @Resource(name = "clientsDataSource")
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void createClient2(int id, String name) {
    log.info("Creating client '%s', not supporting transactions.", name);
    if ("Invalid name".equals(name)) {
      throw new IllegalStateException("Invalid name '" + name + "' provided.");
    }
    jdbcTemplate.update("insert into clients (id, name) values(?,?)", id, name);
  }

  @Transactional
  public void createClient(int id, String name) {
    log.info("Creating client '%s' transactionally.", name);
    if ("Invalid name".equals(name)) {
      throw new IllegalStateException("Invalid name '" + name + "' provided.");
    }
    jdbcTemplate.update("insert into clients (id, name) values(?,?)", id, name);
  }
}
