package com.transferwise.common.gaffer.test.suspended.app;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Component("logsService")
public class LogsService {
    @Resource(name = "logsDAO")
    private LogsDAO logsDAO;

    @Resource(name = "idGenerator")
    private IdGenerator idGenerator;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void appendLogNotSupported(String message) {
        logsDAO.createLogMessage(idGenerator.next(), message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void appendError(String message) {
        logsDAO.createLogMessage(idGenerator.next(), message);
    }
}
