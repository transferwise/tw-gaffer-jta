package com.transferwise.common.gaffer.test.complextest1.app;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component("config")
public class Config {

  private boolean createPassword = true;
  private boolean failPasswordCreation;
  private boolean useHibernate = false;

}
