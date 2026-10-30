package mt.generator.mybatis;

import mt.common.config.CommonProperties;
import mt.generator.mybatis.core.dialect.PostgreSqlDialect;
import mt.generator.mybatis.core.model.ForeignKeyDefinition;
import mt.generator.mybatis.core.model.TableDefinition;
import mt.generator.mybatis.core.scanner.EntityScanner;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 把 PostgreSQL DDL 导出到 target/pg-ddl.sql，用于在真实数据库上验证语法。
 *
 * @author Martin
 */
class PostgreSqlDdlDumpTest {

	@Test
	void dumpDdl() throws Exception {
		EntityScanner scanner = new EntityScanner(new CommonProperties());
		PostgreSqlDialect dialect = new PostgreSqlDialect();
		StringBuilder sb = new StringBuilder();
		List<ForeignKeyDefinition> foreignKeys = new ArrayList<>();
		for (Class<?> entityClass : scanner.scan(new String[]{"mt.generator.mybatis.testentity"})) {
			TableDefinition table = scanner.toTableDefinition(entityClass);
			sb.append("-- table: ").append(table.getName()).append('\n');
			for (String sql : dialect.renderCreateTable(table)) {
				sb.append(sql).append(";\n");
			}
			for (String sql : dialect.renderIndexes(table)) {
				sb.append(sql).append(";\n");
			}
			foreignKeys.addAll(table.getForeignKeys());
		}
		sb.append("-- foreign keys\n");
		for (ForeignKeyDefinition foreignKey : foreignKeys) {
			sb.append(dialect.renderForeignKey(foreignKey)).append(";\n");
		}
		Path target = Paths.get("target", "pg-ddl.sql");
		Files.createDirectories(target.getParent());
		Files.write(target, sb.toString().getBytes(StandardCharsets.UTF_8));
		System.out.println("=== generated ddl ===");
		System.out.println(sb);
	}
}
