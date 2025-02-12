package com.transferwise.common.gaffer;

import com.transferwise.common.gaffer.jdbc.AutoCommitStrategy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GafferJtaProperties {

  /**
   * Specifies transaction length, after which we will "ping" connections, before starting the commit routine.
   */
  @NotNull
  private Duration beforeCommitValidationRequiredTime = Duration.ofSeconds(15);

  /**
   * Used to validate if Gaffer integrates correctly to a specific framework.
   *
   * <p>Use Java's single-threaded Cleaner subsystem, so be careful if you have exceptionally large number of transactions.
   */
  private boolean trackAbandonedTransactions = false;

  /**
   * Used in UUID generation.
   */
  private String instanceId = "Gaffer";

  /**
   * Sometimes calling API like Spring will not show any exceptions during rollbacks and commits, so logging those out in gaffer can be necessary.
   */
  private boolean logExceptions = false;


  @Valid
  private Map<String, DatabaseProperties> databases = new ConcurrentHashMap<>();

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
