package mt.generator.mybatis.core.model;

/**
 * 与具体数据库无关的列类型。
 * <p>
 * Java 类型先归一化成该枚举，再由各个 {@link mt.generator.mybatis.core.dialect.Dialect} 映射成具体的 SQL 类型，
 * 这样新增数据库只需要实现类型映射，不需要重复解析实体。
 *
 * @author Martin
 */
public enum ColumnType {
	/**
	 * 整型
	 */
	INTEGER,
	/**
	 * 长整型
	 */
	LONG,
	/**
	 * 短整型
	 */
	SHORT,
	/**
	 * 单字节整型
	 */
	BYTE,
	/**
	 * 布尔
	 */
	BOOLEAN,
	/**
	 * 双精度浮点
	 */
	DOUBLE,
	/**
	 * 单精度浮点
	 */
	FLOAT,
	/**
	 * 高精度数值
	 */
	DECIMAL,
	/**
	 * 字符串
	 */
	STRING,
	/**
	 * 长文本
	 */
	TEXT,
	/**
	 * 二进制
	 */
	BINARY,
	/**
	 * 日期
	 */
	DATE,
	/**
	 * 时间
	 */
	TIME,
	/**
	 * 时间戳
	 */
	TIMESTAMP,
	/**
	 * JSON
	 */
	JSON,
	/**
	 * UUID
	 */
	UUID,
	/**
	 * 枚举
	 */
	ENUM
}
