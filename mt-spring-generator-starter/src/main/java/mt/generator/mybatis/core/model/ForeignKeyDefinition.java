package mt.generator.mybatis.core.model;

import lombok.Builder;
import lombok.Data;
import mt.common.annotation.ForeignKey;

/**
 * 外键定义（与具体数据库无关的中间模型）。
 *
 * @author Martin
 */
@Data
@Builder
public class ForeignKeyDefinition {

	/**
	 * 外键名
	 */
	private String name;

	/**
	 * 从表
	 */
	private String tableName;

	/**
	 * 从表列
	 */
	private String columnName;

	/**
	 * 主表
	 */
	private String referenceTableName;

	/**
	 * 主表列
	 */
	private String referencedColumnName;

	/**
	 * 级联类型
	 */
	@Builder.Default
	private ForeignKey.CascadeType cascadeType = ForeignKey.CascadeType.DEFAULT;
}
