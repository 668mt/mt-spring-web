package mt.generator.mybatis.core.model;

import lombok.Builder;
import lombok.Data;
import mt.generator.mybatis.annotation.IndexType;

import java.util.List;

/**
 * 索引定义（与具体数据库无关的中间模型）。
 *
 * @author Martin
 */
@Data
@Builder
public class IndexDefinition {

	/**
	 * 索引名，为空时由方言按规则生成
	 */
	private String name;

	/**
	 * 索引列
	 */
	private List<String> columns;

	/**
	 * 索引类型
	 */
	@Builder.Default
	private IndexType type = IndexType.normal;
}
