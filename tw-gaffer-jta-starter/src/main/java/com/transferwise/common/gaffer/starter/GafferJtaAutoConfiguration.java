package com.transferwise.common.gaffer.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration(before = {TransactionAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
@Import(GafferJtaConfiguration.class)
public class GafferJtaAutoConfiguration {

}
