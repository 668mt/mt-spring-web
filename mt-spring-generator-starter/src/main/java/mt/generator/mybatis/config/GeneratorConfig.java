package mt.generator.mybatis.config;

import mt.generator.mybatis.Generator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * @Author Martin
 * @Date 2018/11/3
 */
@ComponentScan("mt.generator.mybatis.utils")
public class GeneratorConfig {
	@Bean
	public Generator generator() {
		return new Generator();
	}
}
