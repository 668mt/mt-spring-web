package mt.spring.jwt.annotation;

import mt.spring.jwt.JwtAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import({JwtAutoConfiguration.class})
public @interface EnableJwt {
}
