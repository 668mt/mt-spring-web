package mt.common.annotation;


import mt.common.config.IdGeneratorConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@EnableDataLock
@Import({IdGeneratorConfiguration.class})
public @interface EnableIdGenerator {

}
