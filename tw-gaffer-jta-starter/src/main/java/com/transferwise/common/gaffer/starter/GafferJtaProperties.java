package com.transferwise.common.gaffer.starter;

import com.transferwise.common.gaffer.jdbc.AutoCommitStrategy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.validation.annotation.Validated;

@Data
@Accessors(chain = true)
@Validated
public class GafferJtaProperties {

  @Valid
  private Map<String, DatabaseProperties> databases = new ConcurrentHashMap<>();

  @NotNull
  private Duration beforeCommitValidationRequiredTime = Duration.ofSeconds(15);

  @Data
  @Accessors(chain = true)
  public static class DatabaseProperties {

    private int commitOrder = 0;

    @NotNull
    private AutoCommitStrategy autoCommitStrategy = AutoCommitStrategy.NONE;

    @NotNull
    private Duration connectionValidationInterval = Duration.ofSeconds(15);

    private boolean registerAsMbean = false;

    private boolean instrumentWithSpringIntegrationAdapter = true;
  }
}
