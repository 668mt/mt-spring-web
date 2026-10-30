package mt.generator.mybatis.core.dialect;

import lombok.Getter;

/**
 * JDBC URL 解析结果。
 * <p>
 * 支持以下常见形式：
 * <ul>
 *     <li>{@code jdbc:mysql://127.0.0.1:3306/db?useSSL=false}</li>
 *     <li>{@code jdbc:postgresql://127.0.0.1:5432/db?ssl=true}</li>
 *     <li>{@code jdbc:postgresql://h1:5432,h2:5432/db}（多主机）</li>
 *     <li>{@code jdbc:postgresql:db}（无主机）</li>
 * </ul>
 *
 * @author Martin
 */
@Getter
public class JdbcUrl {

	/**
	 * 协议名，如 mysql、postgresql
	 */
	private final String scheme;

	/**
	 * 主机部分，如 127.0.0.1:5432
	 */
	private final String hosts;

	/**
	 * 数据库名
	 */
	private final String database;

	/**
	 * 连接参数
	 */
	private final String params;

	private JdbcUrl(String scheme, String hosts, String database, String params) {
		this.scheme = scheme;
		this.hosts = hosts;
		this.database = database;
		this.params = params;
	}

	/**
	 * 解析 JDBC URL
	 *
	 * @param jdbcUrl jdbc 连接串
	 * @return 解析结果
	 */
	public static JdbcUrl parse(String jdbcUrl) {
		String url = jdbcUrl == null ? "" : jdbcUrl.trim();
		if (url.regionMatches(true, 0, "jdbc:", 0, 5)) {
			url = url.substring(5);
		}
		String scheme = url;
		String remainder = "";
		boolean authority = false;
		int sep = url.indexOf("://");
		if (sep >= 0) {
			scheme = url.substring(0, sep);
			remainder = url.substring(sep + 3);
			authority = true;
		} else {
			int colon = url.indexOf(':');
			if (colon >= 0) {
				scheme = url.substring(0, colon);
				remainder = url.substring(colon + 1);
			}
		}
		String params = "";
		int q = remainder.indexOf('?');
		if (q >= 0) {
			params = remainder.substring(q + 1);
			remainder = remainder.substring(0, q);
		}
		String hosts = "";
		String database = "";
		if (authority) {
			int slash = remainder.indexOf('/');
			if (slash >= 0) {
				hosts = remainder.substring(0, slash);
				database = remainder.substring(slash + 1);
			} else {
				hosts = remainder;
			}
		} else {
			// jdbc:postgresql:dbname 这种没有主机部分的写法，剩余部分就是库名
			database = remainder;
		}
		return new JdbcUrl(scheme, hosts, database, params);
	}

	/**
	 * 重新拼装 JDBC URL
	 *
	 * @param scheme   协议名
	 * @param hosts    主机部分
	 * @param database 数据库名
	 * @param params   连接参数
	 * @return jdbc 连接串
	 */
	public static String build(String scheme, String hosts, String database, String params) {
		StringBuilder sb = new StringBuilder("jdbc:").append(scheme);
		boolean hasHost = hosts != null && !hosts.isEmpty();
		if (hasHost) {
			sb.append("://").append(hosts);
		}
		if (database != null && !database.isEmpty()) {
			// 没有主机部分时用 jdbc:postgresql:dbname 的写法
			sb.append(hasHost ? "/" : ":").append(database);
		}
		if (params != null && !params.isEmpty()) {
			sb.append('?').append(params);
		}
		return sb.toString();
	}
}
