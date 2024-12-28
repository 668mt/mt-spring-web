package mt.common.mybatis.mapper;

import mt.common.mybatis.sqlProvider.BaseUpdateProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.UpdateProvider;
import tk.mybatis.mapper.annotation.RegisterMapper;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.entity.Example;

/**
 * 自定义通用mapper
 */
@RegisterMapper
public interface BaseUpdateMapper<T> extends Mapper<T> {
	
	/**
	 * 在某个字段上新增：update xx set xx = xx + #{value} where #{example}
	 *
	 * @param field 字段名
	 * @param value 值
	 * @return 返回结果
	 */
	@UpdateProvider(type = BaseUpdateProvider.class, method = "dynamicSQL")
	int addField(@Param("field") String field, @Param("value") int value, @Param("example") Example example);
}
