package com.transferwise.common.gaffer.test.complextest1.app;

import com.google.common.base.Throwables;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository("clientsDAO")
public class ClientsDao {

  @Resource(name = "accountsDataSource")
  private DataSource accountsDataSource;

  private JdbcTemplate jdbcTemplate;

  @Resource(name = "clientsSessionFactory")
  private SessionFactory sessionFactory;

  @Resource(name = "clientsDataSource")
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Resource(name = "config")
  private Config config;

  public void createClient(int id, String name) {
    jdbcTemplate.update("insert into clients (id, name) values(?,?)", id, name);
    checkIfHibernateIsUsingSameConnection(name);
  }

  private void checkIfHibernateIsUsingSameConnection(String name) {
    if (config.isUseHibernate()) {
      Client client = (Client) sessionFactory.getCurrentSession().createQuery("from Client as client where client.name=?").setString(0, name)
          .uniqueResult();

      if (client == null || !client.getName().equals(name)) {
        throw new IllegalStateException("Hibernate can not find client from the current transaction. Client is '" + client + "'.");
      }
    }
  }

  @Transactional
  public void deleteClient(String name) {
    try {
      jdbcTemplate.update("delte from clients where name = (?)", name);
    } catch (DataAccessException ex) {
      throw new ClientNotFoundException("Client " + name + " not found ");
    }
  }

  @Transactional
  public void deleteAccount(String referenceNumber) {
    try (Connection con = DataSourceUtils.getConnection(accountsDataSource);
        PreparedStatement stmt = con.prepareStatement("delete from accounts where referenceNumber = ?")) {
      stmt.setString(1, referenceNumber);
      stmt.execute();
    } catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  @Transactional
  public void createAccount(int id, int clientId, String referenceNumber) {
    try (Connection con = DataSourceUtils.getConnection(accountsDataSource);
        PreparedStatement stmt = con.prepareStatement("insert into accounts (id, clientId, referenceNumber) values(?,?,?)")) {
      stmt.setInt(1, id);
      stmt.setInt(2, clientId);
      stmt.setString(3, referenceNumber);
      stmt.execute();
    } catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }
}
