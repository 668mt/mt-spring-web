package mt.generator.mybatis.config;

import mt.generator.mybatis.Generator;
import mt.generator.mybatis.core.dialect.MySqlDialect;
import mt.generator.mybatis.core.dialect.PostgreSqlDialect;
import mt.generator.mybatis.core.dialect.SqlServerDialect;
import mt.generator.mybatis.utils.MysqlParser;
import mt.generator.mybatis.utils.PostgresqlParser;
import mt.generator.mybatis.utils.SqlServerParser;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 代码生成器自动配置。
 *
 * @author Martin
 * @Date 2018/11/3
 */
@AutoConfiguration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class GeneratorConfig {

	/**
	 * 生成器
	 * <p>
	 * 这里不用 {@code @ConditionalOnProperty} 判断开关，
	 * 因为配置有可能在上下文刷新之后才加载（比如 Nacos），
	 * 真正的开关判断放在 {@link Generator} 监听上下文刷新的时候做。
	 *
	 * @return 生成器
	 */
	@Bean
	@ConditionalOnMissingBean
	public Generator generator() {
		return new Generator();
	}

	@Bean
	@ConditionalOnMissingBean
	public MySqlDialect mySqlDialect() {
		return new MySqlDialect();
	}

	@Bean
	@ConditionalOnMissingBean
	public PostgreSqlDialect postgreSqlDialect() {
		return new PostgreSqlDialect();
	}

	@Bean
	@ConditionalOnMissingBean
	public SqlServerDialect sqlServerDialect() {
		return new SqlServerDialect();
	}

	/**
	 * @return MySQL 解析器
	 * @deprecated 请直接使用 {@link MySqlDialect}
	 */
	@Deprecated
	@Bean
	@ConditionalOnMissingBean
	public MysqlParser mysqlParser() {
		return new MysqlParser();
	}

	/**
	 * @return PostgreSQL 解析器
	 * @deprecated 请直接使用 {@link PostgreSqlDialect}
	 */
	@Deprecated
	@Bean
	@ConditionalOnMissingBean
	public PostgresqlParser postgresqlParser() {
		return new PostgresqlParser();
	}

	/**
	 * @return SQL Server 解析器
	 * @deprecated 请直接使用 {@link SqlServerDialect}
	 */
	@Deprecated
	@Bean
	@ConditionalOnMissingBean
	public SqlServerParser sqlServerParser() {
		return new SqlServerParser();
	}
}
