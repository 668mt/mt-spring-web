package mt.generator.mybatis;

import mt.common.config.CommonProperties;
import mt.generator.mybatis.config.GeneratorConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import tk.mybatis.mapper.autoconfigure.MybatisProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 建表和定时任务的竞态测试。
 * <p>
 * {@code ScheduledAnnotationBeanPostProcessor} 在 prepareRefresh 阶段就注册成事件监听器，
 * 而 {@code @EventListener} 方法要等所有单例创建完才注册，两者默认都是 LOWEST_PRECEDENCE，
 * 排序后定时任务会先启动，导致定时任务查还没建出来的表。
 * 这里用真实定时任务验证建表一定发生在定时任务启动之前。
 *
 * @author Martin
 */
@EnabledIfEnvironmentVariable(named = "MT_GENERATOR_PG_URL", matches = ".+")
class GeneratorStartupOrderTest {

	@Test
	void scheduledTaskMustNotRunBeforeTablesAreCreated() throws Exception {
		String url = System.getenv("MT_GENERATOR_PG_URL");
		String user = System.getenv("MT_GENERATOR_PG_USER");
		String password = System.getenv("MT_GENERATOR_PG_PASSWORD");
		String database = url.substring(url.lastIndexOf('/') + 1);

		// 清掉上一轮的表，否则表一直存在，测不出竞态
		dropDatabase(url, user, password, database);

		DataSourceProperties properties = new DataSourceProperties();
		properties.setUrl(url);
		properties.setUsername(user);
		properties.setPassword(password);
		properties.setDriverClassName("org.postgresql.Driver");

		CommonProperties commonProperties = new CommonProperties();
		commonProperties.getGenerator().setEnabled(true);
		commonProperties.getGenerator().setPackages(List.of("mt.generator.mybatis.testentity"));

		AtomicBoolean tableExistsOnFirstRun = new AtomicBoolean(false);
		CountDownLatch latch = new CountDownLatch(1);

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.registerBean(DataSourceProperties.class, () -> properties);
			context.registerBean(CommonProperties.class, () -> commonProperties);
			context.registerBean(MybatisProperties.class, MybatisProperties::new);
			context.register(GeneratorConfig.class);
			context.register(SchedulingConfig.class);
			context.registerBean("orderTestTask", OrderTestTask.class,
					() -> new OrderTestTask(url, user, password, database, tableExistsOnFirstRun, latch));
			context.refresh();
			assertTrue(latch.await(60, TimeUnit.SECONDS), "定时任务没有在预期时间内执行");
		}

		assertTrue(tableExistsOnFirstRun.get(),
				"定时任务首次执行时表还没建出来，建表和定时任务启动之间存在竞态");
	}

	/**
	 * 定时任务首次执行时检查表是否已经存在
	 */
	static class OrderTestTask {

		private final String url;

		private final String user;

		private final String password;

		private final String database;

		private final AtomicBoolean firstRun;

		private final CountDownLatch latch;

		OrderTestTask(String url, String user, String password, String database, AtomicBoolean firstRun,
					  CountDownLatch latch) {
			this.url = url;
			this.user = user;
			this.password = password;
			this.database = database;
			this.firstRun = firstRun;
			this.latch = latch;
		}

		@Scheduled(fixedDelay = 200)
		public void run() {
			if (!firstRun.compareAndSet(false, true)) {
				return;
			}
			try (Connection connection = DriverManager.getConnection(url, user, password);
				 Statement statement = connection.createStatement();
				 ResultSet rs = statement.executeQuery(
						 "SELECT count(*) FROM information_schema.tables WHERE table_name = 'notice'")) {
				rs.next();
				firstRun.set(rs.getInt(1) > 0);
			} catch (Exception e) {
				firstRun.set(false);
			} finally {
				latch.countDown();
			}
		}
	}

	@Configuration
	@EnableScheduling
	static class SchedulingConfig {
	}

	private void dropDatabase(String url, String user, String password, String database) throws Exception {
		String maintenanceUrl = url.substring(0, url.lastIndexOf('/') + 1) + "postgres";
		try (Connection connection = DriverManager.getConnection(maintenanceUrl, user, password);
			 Statement statement = connection.createStatement()) {
			statement.execute("DROP DATABASE IF EXISTS " + database + " WITH (FORCE)");
		}
	}
}
