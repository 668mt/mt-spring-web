package mt.generator.mybatis.core.dialect;

import mt.generator.mybatis.core.model.ColumnDefinition;
import mt.generator.mybatis.core.model.ColumnType;
import mt.generator.mybatis.core.model.IndexDefinition;
import mt.generator.mybatis.core.model.TableDefinition;
import mt.generator.mybatis.annotation.IndexType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MySQL 方言测试，保证重构后行为不回退
 *
 * @author Martin
 */
class MySqlDialectTest {

	private final MySqlDialect dialect = new MySqlDialect();

	/**
	 * 渲染简单类型
	 *
	 * @param columnType 列类型
	 * @return SQL 类型
	 */
	private String type(ColumnType columnType) {
		return dialect.renderColumnType(ColumnDefinition.builder()
				.javaType(Object.class).columnType(columnType).build());
	}

	@Test
	void supportsDriver() {
		assertTrue(dialect.supports("com.mysql.cj.jdbc.Driver"));
		assertTrue(dialect.supports("com.mysql.jdbc.Driver"));
		assertFalse(dialect.supports("org.postgresql.Driver"));
	}

	@Test
	void parseDatabaseName() {
		assertEquals("mt_spring", dialect.getDatabaseName("jdbc:mysql://127.0.0.1:3306/mt_spring?useSSL=false"));
	}

	@Test
	void maintenanceJdbcUrl() {
		assertEquals("jdbc:mysql://127.0.0.1:3306/mysql?useSSL=false",
				dialect.getMaintenanceJdbcUrl("jdbc:mysql://127.0.0.1:3306/mt_spring?useSSL=false"));
	}

	@Test
	void columnTypes() {
		assertEquals("int", type(ColumnType.INTEGER));
		assertEquals("bigint", type(ColumnType.LONG));
		assertEquals("int", type(ColumnType.BOOLEAN));
		assertEquals("datetime", type(ColumnType.TIMESTAMP));
		assertEquals("blob", type(ColumnType.BINARY));
	}

	@Test
	void identityClause() {
		ColumnDefinition column = ColumnDefinition.builder()
				.javaType(Long.class).columnType(ColumnType.LONG).autoIncrement(true).build();
		assertEquals("AUTO_INCREMENT", dialect.renderIdentityClause(column));
	}

	@Test
	void identifiersAreAlwaysQuoted() {
		assertEquals("`user`", dialect.quoteIdentifier("user"));
		assertEquals("`userId`", dialect.quoteIdentifier("userId"));
	}

	@Test
	void createTable() {
		TableDefinition table = TableDefinition.builder()
				.name("notice")
				.columns(Arrays.asList(
						ColumnDefinition.builder().name("id").javaType(Long.class).columnType(ColumnType.LONG)
								.autoIncrement(true).nullable(false).build(),
						ColumnDefinition.builder().name("title").javaType(String.class).columnType(ColumnType.STRING)
								.length(255).nullable(false).build()))
				.primaryKeyColumns(Collections.singletonList("id"))
				.build();
		String sql = dialect.renderCreateTable(table).get(0);
		assertTrue(sql.startsWith("CREATE TABLE `notice` ("), sql);
		assertTrue(sql.contains("`id` bigint AUTO_INCREMENT NOT NULL"), sql);
		assertTrue(sql.contains("`title` varchar(255) NOT NULL"), sql);
		assertTrue(sql.contains("PRIMARY KEY (`id`)"), sql);
	}

	@Test
	void fullTextIndexUsesNgramParser() {
		TableDefinition table = TableDefinition.builder()
				.name("notice")
				.indexes(Collections.singletonList(IndexDefinition.builder()
						.name("idx_notice_content").columns(Collections.singletonList("content"))
						.type(IndexType.fulltext).build()))
				.build();
		List<String> sqls = dialect.renderIndexes(table);
		assertEquals("CREATE FULLTEXT INDEX `idx_notice_content` ON `notice` (`content`) WITH PARSER ngram", sqls.get(0));
	}

	@Test
	void indexNameDoesNotContainTableName() {
		TableDefinition table = TableDefinition.builder()
				.name("notice")
				.indexes(Collections.singletonList(IndexDefinition.builder()
						.name(null).columns(Collections.singletonList("userId")).type(IndexType.normal).build()))
				.build();
		assertEquals("CREATE INDEX `INDEX_userId` ON `notice` (`userId`)", dialect.renderIndexes(table).get(0));
	}
}
