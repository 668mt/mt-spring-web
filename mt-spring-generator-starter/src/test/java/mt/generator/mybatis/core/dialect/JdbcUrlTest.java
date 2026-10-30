package mt.generator.mybatis.core.dialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * JDBC URL 解析测试
 *
 * @author Martin
 */
class JdbcUrlTest {

	@Test
	void mysqlUrl() {
		JdbcUrl url = JdbcUrl.parse("jdbc:mysql://127.0.0.1:3306/mt_spring?useSSL=false&serverTimezone=UTC");
		assertEquals("mysql", url.getScheme());
		assertEquals("127.0.0.1:3306", url.getHosts());
		assertEquals("mt_spring", url.getDatabase());
		assertEquals("useSSL=false&serverTimezone=UTC", url.getParams());
	}

	@Test
	void postgresUrl() {
		JdbcUrl url = JdbcUrl.parse("jdbc:postgresql://127.0.0.1:5432/rec?ssl=true");
		assertEquals("postgresql", url.getScheme());
		assertEquals("127.0.0.1:5432", url.getHosts());
		assertEquals("rec", url.getDatabase());
		assertEquals("ssl=true", url.getParams());
	}

	@Test
	void postgresMultiHostUrl() {
		JdbcUrl url = JdbcUrl.parse("jdbc:postgresql://h1:5432,h2:5432/rec");
		assertEquals("h1:5432,h2:5432", url.getHosts());
		assertEquals("rec", url.getDatabase());
	}

	@Test
	void postgresUrlWithoutHost() {
		JdbcUrl url = JdbcUrl.parse("jdbc:postgresql:rec");
		assertEquals("postgresql", url.getScheme());
		assertEquals("rec", url.getDatabase());
	}

	@Test
	void postgresUrlWithoutDatabase() {
		JdbcUrl url = JdbcUrl.parse("jdbc:postgresql://127.0.0.1:5432/");
		assertEquals("", url.getDatabase());
	}

	@Test
	void nullUrl() {
		JdbcUrl url = JdbcUrl.parse(null);
		assertEquals("", url.getScheme());
		assertEquals("", url.getDatabase());
		assertEquals("", url.getParams());
	}

	@Test
	void buildWithHost() {
		assertEquals("jdbc:postgresql://127.0.0.1:5432/postgres", JdbcUrl.build("postgresql", "127.0.0.1:5432", "postgres", null));
	}

	@Test
	void buildWithoutHost() {
		assertEquals("jdbc:postgresql:postgres", JdbcUrl.build("postgresql", "", "postgres", null));
	}

	@Test
	void buildWithParams() {
		assertEquals("jdbc:mysql://127.0.0.1:3306/mysql?useSSL=false",
				JdbcUrl.build("mysql", "127.0.0.1:3306", "mysql", "useSSL=false"));
	}
}
