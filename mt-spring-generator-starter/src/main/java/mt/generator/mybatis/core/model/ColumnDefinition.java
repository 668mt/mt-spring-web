package mt.generator.mybatis.core.model;

import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Field;

/**
 * 列定义（与具体数据库无关的中间模型）。
 *
 * @author Martin
 */
@Data
@Builder
public class ColumnDefinition {

	/**
	 * 列名
	 */
	private String name;

	/**
	 * java 类型
	 */
	private Class<?> javaType;

	/**
	 * 归一化后的列类型
	 */
	private ColumnType columnType;

	/**
	 * 显式指定的列定义（{@code @Column(columnDefinition = "...")}）
	 */
	private String columnDefinition;

	/**
	 * 是否允许为 null
	 */
	@Builder.Default
	private boolean nullable = true;

	/**
	 * 是否唯一
	 */
	@Builder.Default
	private boolean unique = false;

	/**
	 * 是否主键
	 */
	@Builder.Default
	private boolean primaryKey = false;

	/**
	 * 是否自增
	 */
	@Builder.Default
	private boolean autoIncrement = false;

	/**
	 * 长度
	 */
	private int length;

	/**
	 * 精度
	 */
	private int precision;

	/**
	 * 小数位
	 */
	private int scale;

	/**
	 * 来源字段，供方言读取注解使用
	 */
	private transient Field field;
}
