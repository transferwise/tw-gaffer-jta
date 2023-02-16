package com.transferwise.common.gaffer.test.complextest1.app;

import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("clientsService")
public class ClientsService {

  protected static final Logger log = LoggerFactory.getLogger(ClientsService.class);

  @Resource(name = "clientsDAO")
  private ClientsDao clientsDao;

  @Resource(name = "usersService")
  private UsersService usersService;

  @Resource(name = "idGenerator")
  private IdGenerator idGenerator;

  public void createClientLegacy(String clientName) {
    createClientInner(clientName);
  }

  @Transactional
  public void createClient(String clientName) {
    createClientInner(clientName);
  }

  private void createClientInner(String clientName) {
    int clientId = idGenerator.next();
    int accountId = idGenerator.next();
    clientsDao.createClient(clientId, clientName);
    clientsDao.createAccount(accountId, clientId, "666");

    usersService.createUser(clientId, "bemmimehed@tallinn.ee");
  }

  @Transactional
  public void deleteClient(String clientName) {
    try {
      clientsDao.deleteClient(clientName);
    } catch (ClientNotFoundException ex) {
      log.error("client not found", ex);
    }
    clientsDao.deleteAccount("666");
  }
}
