package com.transferwise.common.gaffer.test.testsuitea;

import com.transferwise.common.gaffer.test.BaseExtension;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ActiveProfiles(profiles = {"integration"})
@ExtendWith(BaseExtension.class)
@SpringBootTest(classes = {TestApplication.class})
@TestInstance(Lifecycle.PER_CLASS)
public @interface BaseTestEnvironment {

}
