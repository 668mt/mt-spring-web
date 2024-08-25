package mt.common.mybatis.sqlProvider;

import mt.common.mybatis.utils.SqlProviderUtils;
import org.apache.ibatis.mapping.MappedStatement;
import tk.mybatis.mapper.mapperhelper.MapperHelper;
import tk.mybatis.mapper.mapperhelper.MapperTemplate;
import tk.mybatis.mapper.mapperhelper.SqlHelper;

import static tk.mybatis.mapper.mapperhelper.SqlHelper.getDynamicTableName;

/**
 * 自定义公用方法
 *
 * @author Martin
 * @date 2017-10-23 下午7:15:59
 */
public class BaseUpdateProvider extends MapperTemplate {
	
	public BaseUpdateProvider(Class<?> mapperClass, MapperHelper mapperHelper) {
		super(mapperClass, mapperHelper);
	}
	
	public String addField(MappedStatement ms) {
		Class<?> entityClass = getEntityClass(ms);
		
		String tableName = getDynamicTableName(entityClass, tableName(entityClass));
		return "update " + tableName + " set ${field} = ${field} + #{value}" +
			SqlProviderUtils.exampleWhereClause("example");
	}
	
}
