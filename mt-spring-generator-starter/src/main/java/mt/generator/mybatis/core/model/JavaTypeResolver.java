package mt.generator.mybatis.core.model;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Java 类型 -> {@link ColumnType} 的归一化解析。
 * <p>
 * 注意原始实现用的是 {@code type.isAssignableFrom(X.class)}，方向是反的，
 * 导致基本类型（int/boolean 等）全部退化成 varchar，这里统一改成显式匹配。
 *
 * @author Martin
 */
public final class JavaTypeResolver {

	private JavaTypeResolver() {
	}

	/**
	 * 解析列类型
	 *
	 * @param type java 类型
	 * @return 列类型
	 */
	public static ColumnType resolve(Class<?> type) {
		if (type == null) {
			return ColumnType.STRING;
		}
		if (type == boolean.class || type == Boolean.class) {
			return ColumnType.BOOLEAN;
		}
		if (type == byte.class || type == Byte.class) {
			return ColumnType.BYTE;
		}
		if (type == short.class || type == Short.class) {
			return ColumnType.SHORT;
		}
		if (type == int.class || type == Integer.class) {
			return ColumnType.INTEGER;
		}
		if (type == long.class || type == Long.class) {
			return ColumnType.LONG;
		}
		if (type == float.class || type == Float.class) {
			return ColumnType.FLOAT;
		}
		if (type == double.class || type == Double.class) {
			return ColumnType.DOUBLE;
		}
		if (BigDecimal.class.isAssignableFrom(type) || BigInteger.class.isAssignableFrom(type)) {
			return ColumnType.DECIMAL;
		}
		if (type == byte[].class || type == Byte[].class) {
			return ColumnType.BINARY;
		}
		// java.sql.Date / java.sql.Time / java.sql.Timestamp 都是 java.util.Date 的子类，必须先判断
		if (type == java.sql.Date.class || type == java.time.LocalDate.class) {
			return ColumnType.DATE;
		}
		if (type == java.sql.Time.class || type == java.time.LocalTime.class) {
			return ColumnType.TIME;
		}
		if (type == java.util.Date.class || type == java.sql.Timestamp.class
				|| type == java.time.LocalDateTime.class || type == java.time.Instant.class
				|| type == java.time.ZonedDateTime.class || type == java.time.OffsetDateTime.class) {
			return ColumnType.TIMESTAMP;
		}
		if (type == java.util.UUID.class) {
			return ColumnType.UUID;
		}
		if (CharSequence.class.isAssignableFrom(type) || type == char.class || type == Character.class) {
			return ColumnType.STRING;
		}
		if (Enum.class.isAssignableFrom(type)) {
			return ColumnType.ENUM;
		}
		if (isJson(type)) {
			return ColumnType.JSON;
		}
		return ColumnType.STRING;
	}

	/**
	 * 通过类名判断是否为 JSON 类型，避免依赖具体的 JSON 库
	 *
	 * @param type java 类型
	 * @return 是否为 JSON 类型
	 */
	private static boolean isJson(Class<?> type) {
		String name = type.getName();
		return name.endsWith("JSONObject") || name.endsWith("JSONArray") || name.endsWith("JsonNode");
	}
}
