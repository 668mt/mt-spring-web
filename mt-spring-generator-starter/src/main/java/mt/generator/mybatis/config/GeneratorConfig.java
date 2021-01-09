package mt.generator.mybatis.config;

import mt.generator.mybatis.Generator;
import mt.generator.mybatis.utils.MysqlParser;
import mt.generator.mybatis.utils.SqlServerParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @Author Martin
 * @Date 2018/11/3
 */
public class GeneratorConfig {
	@Bean
	public Generator generator() {
		return new Generator();
	}
	
	@Bean
	@ConditionalOnMissingBean(MysqlParser.class)
	public MysqlParser mysqlParser() {
		return new MysqlParser();
	}
	
	@Bean
	@ConditionalOnMissingBean(SqlServerParser.class)
	public SqlServerParser sqlServerParser() {
		return new SqlServerParser();
	}
}
