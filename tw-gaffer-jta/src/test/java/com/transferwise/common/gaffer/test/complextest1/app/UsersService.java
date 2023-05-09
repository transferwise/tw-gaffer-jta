package com.transferwise.common.gaffer.test.complextest1.app;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("usersService")
public class UsersService {

  @Resource(name = "usersDAO")
  private UsersDao usersDao;

  @Resource(name = "config")
  private Config config;

  @Resource(name = "idGenerator")
  private IdGenerator idGenerator;

  @Transactional
  public void createUser(int clientId, String email) {
    int userId = idGenerator.next();
    int passwordId = idGenerator.next();
    usersDao.createUser(userId, clientId, email);

    if (config.isCreatePassword()) {
      usersDao.createPassword(passwordId, userId, "Salasona");
    }
  }
}
