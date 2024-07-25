package mt.common.mybatis.sqlProvider;

import mt.common.mybatis.entity.GroupCount;
import mt.common.mybatis.utils.SqlProviderUtils;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.reflection.MetaObject;
import tk.mybatis.mapper.entity.EntityTable;
import tk.mybatis.mapper.mapperhelper.EntityHelper;
import tk.mybatis.mapper.mapperhelper.MapperHelper;
import tk.mybatis.mapper.mapperhelper.MapperTemplate;
import tk.mybatis.mapper.mapperhelper.SqlHelper;
import tk.mybatis.mapper.util.MetaObjectUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自定义公用方法
 *
 * @author Martin
 * @date 2017-10-23 下午7:15:59
 */
public class BaseSelectProvider extends MapperTemplate {
	
	public BaseSelectProvider(Class<?> mapperClass, MapperHelper mapperHelper) {
		super(mapperClass, mapperHelper);
	}
	
	/**
	 * 是否存在
	 *
	 * @param ms
	 * @return
	 */
	public String existsKeyValue(MappedStatement ms) {
		Class<?> entityClass = getEntityClass(ms);
		StringBuilder sql = new StringBuilder();
		sql.append(SqlHelper.selectCountExists(entityClass));
		sql.append(SqlHelper.fromTable(entityClass, tableName(entityClass)));
		sql.append("where ${@mt.common.mybatis.utils.MapperColumnUtils@parseColumn(columnName,'");
		sql.append(entityClass.getName());
		sql.append("')} = #{value}");
		
		return sql.toString();
	}
	
	/**
	 * 查找
	 *
	 * @param ms
	 * @return
	 */
	public String findOne(MappedStatement ms) {
		Class<?> entityClass = getEntityClass(ms);
		//修改返回值类型为实体类型
		setResultType(ms, entityClass);
		
		StringBuilder sql = new StringBuilder();
		sql.append(SqlHelper.selectAllColumns(entityClass));
		sql.append(SqlHelper.fromTable(entityClass, tableName(entityClass)));
		sql.append("where ${@mt.common.mybatis.utils.MapperColumnUtils@parseColumn(columnName,'");
		sql.append(entityClass.getName());
		sql.append("')} = #{value}");
		sql.append(SqlHelper.orderByDefault(entityClass));
		return sql.toString();
	}
	
	/**
	 * 查找
	 *
	 * @param ms
	 * @return
	 */
	public String findList(MappedStatement ms) {
		Class<?> entityClass = getEntityClass(ms);
		//修改返回值类型为实体类型
		setResultType(ms, entityClass);
		
		StringBuilder sql = new StringBuilder();
		sql.append(SqlHelper.selectAllColumns(entityClass));
		sql.append(SqlHelper.fromTable(entityClass, tableName(entityClass)));
		sql.append("where ${@mt.common.mybatis.utils.MapperColumnUtils@parseColumn(columnName,'");
		sql.append(entityClass.getName());
		sql.append("')} = #{value}");
		sql.append(SqlHelper.orderByDefault(entityClass));
		return sql.toString();
	}
	
	public String findGroupCounts(MappedStatement ms) {
		Class<?> entityClass = getEntityClass(ms);
		
		return "select ${@mt.common.mybatis.utils.CheckUtils@mustOneField(" +
			"@mt.common.mybatis.utils.MapperColumnUtils@parseColumn(key,'" + entityClass.getName() + "')" +
			")} as field, count(0) as value" +
			SqlHelper.fromTable(entityClass, tableName(entityClass)) +
			SqlProviderUtils.exampleWhereClause("example") +
			"group by ${@mt.common.mybatis.utils.CheckUtils@mustOneField(" +
			"@mt.common.mybatis.utils.MapperColumnUtils@parseColumn(key,'" + entityClass.getName() + "')" +
			")}";
	}
	
}
