package com.transferwise.common.gaffer.starter;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(GafferJtaConfiguration.class)
@AutoConfigureBefore({TransactionAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
public class GafferJtaAutoConfiguration {

}
