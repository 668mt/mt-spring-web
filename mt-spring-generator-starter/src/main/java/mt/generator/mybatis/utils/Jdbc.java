package mt.generator.mybatis.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC 访问实现。
 *
 * @author Martin
 * @ClassName: Jdbc
 */
public class Jdbc extends JdbcAbstract {

	public Jdbc(String driverClass, String jdbcUrl, String user, String password) {
		super(driverClass, jdbcUrl, user, password);
	}

	@Override
	List<Map<String, Object>> select(String sql, Connection conn) {
		List<Map<String, Object>> list = new ArrayList<>();
		if (conn == null) {
			throw new RuntimeException("数据库连接失败");
		}
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			ResultSetMetaData metaData = rs.getMetaData();
			int col = metaData.getColumnCount();
			while (rs.next()) {
				Map<String, Object> map = new LinkedHashMap<>();
				for (int i = 1; i <= col; i++) {
					map.put(metaData.getColumnLabel(i), rs.getObject(i));
				}
				list.add(map);
			}
		} catch (SQLException e) {
			throw new RuntimeException("查询失败：" + sql, e);
		} finally {
			close(ps, rs);
		}
		return list;
	}

	@Override
	int update(String sql, Connection conn) {
		if (conn == null) {
			throw new RuntimeException("数据库连接失败");
		}
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			return ps.executeUpdate();
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException("执行失败：" + sql, e);
		} finally {
			close(ps, null);
		}
	}

}
