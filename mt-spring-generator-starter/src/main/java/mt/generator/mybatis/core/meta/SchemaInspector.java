package mt.generator.mybatis.core.meta;

import mt.generator.mybatis.core.dialect.Dialect;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据库结构探查器。
 * <p>
 * 表、字段统一使用 JDBC 的 {@link java.sql.DatabaseMetaData} 查询，
 * 不再为每个数据库写一段各自的查询 SQL；数据库是否存在由各方言自己判断
 * （PostgreSQL 的驱动 {@code getCatalogs()} 只返回当前库）。
 *
 * @author Martin
 */
public class SchemaInspector {

	private static final Logger log = LoggerFactory.getLogger(SchemaInspector.class);

	private final Dialect dialect;

	public SchemaInspector(Dialect dialect) {
		this.dialect = dialect;
	}

	/**
	 * 数据库是否存在
	 *
	 * @param connection   连接
	 * @param databaseName 数据库名
	 * @return 是否存在
	 */
	public boolean databaseExists(Connection connection, String databaseName) {
		return dialect.databaseExists(connection, databaseName);
	}

	/**
	 * 查找表，返回数据库中真实的表名
	 *
	 * @param connection   连接
	 * @param databaseName 数据库名
	 * @param tableName    表名
	 * @return 表名，不存在返回 null
	 */
	public String findTable(Connection connection, String databaseName, String tableName) {
		if (StringUtils.isBlank(tableName)) {
			return null;
		}
		String schemaPattern = dialect.getSchemaPattern();
		String fallback = null;
		for (String candidate : dialect.getTableNameCandidates(tableName)) {
			try (ResultSet rs = connection.getMetaData()
					.getTables(databaseName, schemaPattern, candidate, new String[]{"TABLE"})) {
				while (rs.next()) {
					String name = rs.getString("TABLE_NAME");
					if (tableName.equals(name)) {
						return name;
					}
					if (fallback == null && tableName.equalsIgnoreCase(name)) {
						fallback = name;
					}
				}
			} catch (SQLException e) {
				throw new IllegalStateException("检查表是否存在失败：" + tableName, e);
			}
		}
		if (fallback != null) {
			log.warn("表 [{}] 在数据库中实际为 [{}]，大小写不一致", tableName, fallback);
		}
		return fallback;
	}

	/**
	 * 查询表已有的字段
	 *
	 * @param connection   连接
	 * @param databaseName 数据库名
	 * @param tableName    表名
	 * @return 字段名集合
	 */
	public Set<String> listColumns(Connection connection, String databaseName, String tableName) {
		Set<String> columns = new LinkedHashSet<>();
		if (StringUtils.isBlank(tableName)) {
			return columns;
		}
		String schemaPattern = dialect.getSchemaPattern();
		List<String> candidates = dialect.getTableNameCandidates(tableName);
		try (ResultSet rs = connection.getMetaData()
				.getColumns(databaseName, schemaPattern, candidates.get(0), null)) {
			while (rs.next()) {
				columns.add(rs.getString("COLUMN_NAME"));
			}
		} catch (SQLException e) {
			throw new IllegalStateException("查询表字段失败：" + tableName, e);
		}
		if (columns.isEmpty() && candidates.size() > 1) {
			try (ResultSet rs = connection.getMetaData()
					.getColumns(databaseName, schemaPattern, candidates.get(1), null)) {
				while (rs.next()) {
					columns.add(rs.getString("COLUMN_NAME"));
				}
			} catch (SQLException e) {
				throw new IllegalStateException("查询表字段失败：" + tableName, e);
			}
		}
		return columns;
	}
}
