package mt.common.mybatis.mapper;

import mt.common.mybatis.entity.GroupCount;
import mt.common.mybatis.sqlProvider.BaseSelectProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import tk.mybatis.mapper.annotation.RegisterMapper;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * 自定义通用mapper
 */
@RegisterMapper
public interface BaseMapper<T> extends Mapper<T> {
	
	/**
	 * 是否存在
	 *
	 * @param columnName
	 * @param value
	 * @return
	 */
	@SelectProvider(type = BaseSelectProvider.class, method = "dynamicSQL")
	@Deprecated
	boolean existsKeyValue(@Param("columnName") String columnName, @Param("value") Object value);
	
	/**
	 * 指定列名查找
	 *
	 * @param columnName 列名
	 * @param value      值
	 * @return 返回结果
	 */
	@SelectProvider(type = BaseSelectProvider.class, method = "dynamicSQL")
	@Deprecated
	T findOne(@Param("columnName") String columnName, @Param("value") Object value);
	
	/**
	 * 指定列名查找
	 *
	 * @param columnName 列名
	 * @param value      值
	 * @return 返回结果列表
	 */
	@SelectProvider(type = BaseSelectProvider.class, method = "dynamicSQL")
	@Deprecated
	List<T> findList(@Param("columnName") String columnName, @Param("value") Object value);
	
	@SelectProvider(type = BaseSelectProvider.class, method = "dynamicSQL")
	List<GroupCount> findGroupCounts(@Param("example") Example example, @Param("key") String key);
}
