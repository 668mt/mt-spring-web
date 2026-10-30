package mt.generator.mybatis.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 表定义（与具体数据库无关的中间模型）。
 * <p>
 * 实体类扫描完成后先转成该模型，再交给 {@link mt.generator.mybatis.core.dialect.Dialect} 渲染成 DDL，
 * 扫描逻辑与 SQL 渲染彻底解耦。
 *
 * @author Martin
 */
@Data
@Builder
public class TableDefinition {

	/**
	 * 表名
	 */
	private String name;

	/**
	 * 列
	 */
	@Builder.Default
	private List<ColumnDefinition> columns = new ArrayList<>();

	/**
	 * 主键列
	 */
	@Builder.Default
	private List<String> primaryKeyColumns = new ArrayList<>();

	/**
	 * 索引
	 */
	@Builder.Default
	private List<IndexDefinition> indexes = new ArrayList<>();

	/**
	 * 外键
	 */
	@Builder.Default
	private List<ForeignKeyDefinition> foreignKeys = new ArrayList<>();
}
