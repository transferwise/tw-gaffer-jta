package com.transferwise.common.gaffer.test.complextest1.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service("usersService")
public class UsersService {
    @Resource(name = "usersDAO")
    private UsersDAO usersDAO;

    @Resource(name = "config")
    private Config config;

    @Resource(name = "idGenerator")
    private IdGenerator idGenerator;

    @Transactional
    public void createUser(int clientId, String email) {
        int userId = idGenerator.next();
        int passwordId = idGenerator.next();
        usersDAO.createUser(userId, clientId, email);

        if (config.isCreatePassword()) {
            usersDAO.createPassword(passwordId, userId, "Salasona");
        }
    }
}
