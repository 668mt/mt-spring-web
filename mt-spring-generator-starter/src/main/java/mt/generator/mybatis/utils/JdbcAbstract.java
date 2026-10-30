package mt.generator.mybatis.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * JDBC 访问抽象。
 * <p>
 * 每次操作单独开连接，用完即关，不做连接池。
 *
 * @author Martin
 * @ClassName: JdbcAbstract
 */
public abstract class JdbcAbstract {

	protected static final Logger log = LoggerFactory.getLogger(JdbcAbstract.class);
	private String driver;

	private String jdbcUrl;

	private String username;

	private String password;

	/**
	 * 查询
	 *
	 * @param sql        select语句
	 * @param connection 连接
	 * @return 结果集
	 */
	abstract List<Map<String, Object>> select(String sql, Connection connection);

	/**
	 * 更新
	 *
	 * @param sql        insert、update、delete语句
	 * @param connection 连接
	 * @return 更新条数
	 */
	abstract int update(String sql, Connection connection);

	/**
	 * 查询
	 *
	 * @param sql   select语句
	 * @param isLog 是否打印日志
	 * @return 结果集
	 */
	public List<Map<String, Object>> select(String sql, boolean isLog) {
		if (StringUtils.isBlank(sql)) {
			throw new RuntimeException("sql语句不能为空");
		}
		if (isLog) {
			log.info("查询语句: {}", sql);
		}
		List<Map<String, Object>> list;
		try (Connection connection = getConnection()) {
			list = select(sql, connection);
		} catch (SQLException e) {
			throw new RuntimeException("查询失败：" + sql, e);
		}
		if (isLog) {
			for (Map<String, Object> map : list) {
				log.info(">>>> row:{}", map);
			}
			log.info(">>>> total:{}", list.size());
		}
		return list;
	}

	/**
	 * 查询
	 *
	 * @param sql select语句
	 * @return 结果集
	 */
	public List<Map<String, Object>> select(String sql) {
		return select(sql, true);
	}

	/**
	 * 查询只有一个结果
	 *
	 * @param sql select语句
	 * @return 结果，没有结果返回 null
	 */
	public Map<String, Object> selectOne(String sql) {
		List<Map<String, Object>> list = select(sql, false);
		if (list.size() > 1) {
			throw new RuntimeException("查询数量超过1");
		}
		if (list.size() == 1) {
			return list.get(0);
		}
		return null;
	}

	/**
	 * 更新
	 *
	 * @param sql update语句
	 * @return 更新条数
	 */
	public int update(String sql) {
		return executeStatement(sql, "更新");
	}

	/**
	 * 删除
	 *
	 * @param sql delete语句
	 * @return 更新条数
	 */
	public int delete(String sql) {
		return executeStatement(sql, "删除");
	}

	/**
	 * 新增
	 *
	 * @param sql insert语句
	 * @return 更新条数
	 */
	public int insert(String sql) {
		return executeStatement(sql, "插入");
	}

	/**
	 * 执行
	 *
	 * @param sql insert、update、delete语句
	 * @return 更新条数
	 */
	public int execute(String sql) {
		return executeStatement(sql, "执行");
	}

	/**
	 * 执行语句
	 *
	 * @param sql    语句
	 * @param action 动作描述，用于日志
	 * @return 更新条数
	 */
	private int executeStatement(String sql, String action) {
		if (StringUtils.isBlank(sql)) {
			throw new RuntimeException("sql语句不能为空");
		}
		log.info("{}语句: {}", action, sql);
		try (Connection connection = getConnection()) {
			int count = update(sql, connection);
			log.info(">>>> {}: {}", action, count);
			return count;
		} catch (SQLException e) {
			throw new RuntimeException(action + "失败：" + sql, e);
		}
	}

	/**
	 * 获取连接，调用方负责关闭
	 *
	 * @return 连接
	 */
	public Connection getConnection() {
		try {
			Class.forName(driver);
			Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
			//DDL 必须自动提交，PostgreSQL 不允许在事务里执行 create database 这类语句
			connection.setAutoCommit(true);
			return connection;
		} catch (Exception e) {
			throw new RuntimeException("数据库连接失败：" + jdbcUrl, e);
		}
	}

	/**
	 * 关闭语句和结果集
	 *
	 * @param ps 语句
	 * @param rs 结果集
	 */
	void close(PreparedStatement ps, ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		try {
			if (ps != null) {
				ps.close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public JdbcAbstract(String driver, String jdbcUrl, String username, String password) {
		super();
		this.driver = driver;
		this.jdbcUrl = jdbcUrl;
		this.username = username;
		this.password = password;
	}

}
