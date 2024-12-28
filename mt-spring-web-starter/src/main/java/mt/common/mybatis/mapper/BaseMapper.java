package mt.common.mybatis.mapper;

import mt.common.mybatis.advanced.AdvancedQuery;
import mt.common.mybatis.entity.GroupCount;
import mt.common.mybatis.sqlProvider.BaseSelectProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import tk.mybatis.mapper.annotation.RegisterMapper;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.Map;

/**
 * 自定义通用mapper
 */
@RegisterMapper
public interface BaseMapper<T> extends Mapper<T>, BaseUpdateMapper<T> {
	
	/**
	 * group by查询
	 *
	 * @param example    查询条件
	 * @param groupByKey group by的字段
	 * @return 返回结果
	 */
	@SelectProvider(type = BaseSelectProvider.class, method = "dynamicSQL")
	List<GroupCount> findGroupCounts(@Param("example") Example example, @Param("key") String groupByKey);
	
	/**
	 * 查询指定字段
	 *
	 * @param fields  查询字段
	 * @param example 查询条件
	 * @return 返回结果
	 */
	@SelectProvider(type = BaseSelectProvider.class, method = "dynamicSQL")
	List<T> findListWithFields(@Param("fields") List<String> fields, @Param("example") Example example);
	
	@SelectProvider(type = BaseSelectProvider.class, method = "dynamicSQL")
	List<Map<String, Object>> findAdvancedList(AdvancedQuery advancedQuery);
}
