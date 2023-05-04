package com.transferwise.common.gaffer.test.suspended.app;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component("logsService")
public class LogsService {

  @Resource(name = "logsDAO")
  private LogsDao logsDao;

  @Resource(name = "idGenerator")
  private IdGenerator idGenerator;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void appendLogNotSupported(String message) {
    logsDao.createLogMessage(idGenerator.next(), message);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void appendError(String message) {
    logsDao.createLogMessage(idGenerator.next(), message);
  }
}
