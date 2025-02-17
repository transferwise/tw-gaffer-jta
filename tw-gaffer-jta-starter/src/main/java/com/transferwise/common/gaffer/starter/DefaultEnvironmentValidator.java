package com.transferwise.common.gaffer.starter;

import com.transferwise.common.gaffer.GafferJtaProperties;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;

public class DefaultEnvironmentValidator implements EnvironmentValidator, ApplicationListener<ApplicationPreparedEvent> {

  @Autowired
  private GafferJtaProperties properties;

  @Autowired
  private Validator validator;

  public void validate() {
    var violations = validator.validate(properties);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  @Override
  public void onApplicationEvent(ApplicationPreparedEvent event) {
    validate();
  }
}
