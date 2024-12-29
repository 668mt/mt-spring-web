package mt.common.service;


import com.github.pagehelper.PageInfo;
import mt.common.entity.Pageable;
import mt.common.mybatis.advanced.AdvancedQuery;
import mt.common.mybatis.entity.GroupCount;
import mt.common.tkmapper.Filter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Martin
 */
public interface BaseRepository<T> {
	
	/**
	 * 返回行数
	 */
	int count(@Nullable List<Filter> filters);
	
	int delete(@NotNull String columnName, @NotNull Object value);
	
	List<Filter> parseCondition(@Nullable Object condition);
	
	<T2> PageInfo<T2> doPage(@NotNull QueryHandler<T2> queryHandler, @Nullable Pageable pageable);
	
	/**
	 * 查询所有
	 */
	List<T> findAll();
	
	PageInfo<T> findPage(@Nullable Pageable pageable);
	
	PageInfo<T> findPage(@Nullable Integer pageNum, @Nullable Integer pageSize, @Nullable String orderBy, @Nullable Object condition);
	
	PageInfo<T> findPage(@Nullable Integer pageNum, @Nullable Integer pageSize, @Nullable String orderBy, @Nullable Object condition, boolean isAllowSelectAll);
	
	List<T> findList(@Nullable Object condition);
	
	<Result> List<Result> findAdvancedList(@NotNull Class<Result> resultClass, @Nullable List<Filter> filters);
	
	<Result> List<Result> findAdvancedList(@NotNull Class<Result> resultClass, @Nullable Filter filter);
	
	<Result> List<Result> findAdvancedList(@NotNull AdvancedQuery advancedQuery);
	
	<Result> PageInfo<Result> findAdvancedPage(@NotNull Class<Result> resultClass, @Nullable Pageable pageable);
	
	/**
	 * 通过id查询
	 *
	 * @return 对象
	 */
	T findById(@NotNull Object record);
	
	T findById(@NotNull Object record, boolean forUpdate);
	
	/**
	 * 查询一个
	 *
	 * @param column
	 * @param value
	 * @return 对象
	 */
	T findOne(@NotNull String column, @NotNull Object value);
	
	List<T> findByFilter(@NotNull Filter filter);
	
	List<T> findByFilter(@NotNull Filter filter, boolean forUpdate);
	
	/**
	 * 查询列表
	 *
	 * @param filters 过滤
	 * @return 列表
	 */
	List<T> findByFilters(@Nullable List<Filter> filters);
	
	/**
	 * 查询列表
	 *
	 * @param filters   过滤
	 * @param forUpdate 是否加锁
	 * @return 列表
	 */
	List<T> findByFilters(@Nullable List<Filter> filters, boolean forUpdate);
	
	T findFirstByFilters(@Nullable List<Filter> filters);
	
	T findFirstByFilter(@NotNull Filter filter);
	
	/**
	 * 查询一个，多个抛出异常
	 */
	T findOneByFilters(@Nullable List<Filter> filters);
	
	T findOneByFilters(@Nullable List<Filter> filters, boolean forUpdate);
	
	T findOneByFilter(@NotNull Filter filter);
	
	T findOneByFilter(@NotNull Filter filter, boolean forUpdate);
	
	/**
	 * 查询列表
	 *
	 * @param column 字段名
	 * @param value  值
	 * @return 列表
	 */
	List<T> findList(@NotNull String column, @NotNull Object value);
	
	/**
	 * 保存,null值会被保存，不会使用数据库默认值
	 *
	 * @param record 对象
	 * @return 影响条数
	 */
	int save(@NotNull T record);
	
	/**
	 * 保存,null值不会被保存，会使用数据库默认值
	 *
	 * @param record 对象
	 * @return 影响条数
	 */
	int saveSelective(@NotNull T record);
	
	/**
	 * 通过主键更新，传入record对象中为null的会被更新
	 *
	 * @param record
	 * @return 影响条数
	 */
	int updateById(@NotNull T record);
	
	/**
	 * 通过主键更新，传入record对象中为null的不会被更新
	 *
	 * @param record
	 * @return 影响条数
	 */
	int updateByIdSelective(@NotNull T record);
	
	int updateByFilters(@NotNull T record, @Nullable List<Filter> filters);
	
	int updateByFilter(@NotNull T record, @NotNull Filter filter);
	
	int updateByFiltersSelective(@NotNull T record, @Nullable List<Filter> filters);
	
	int updateByFilterSelective(@NotNull T record, @NotNull Filter filter);
	
	/**
	 * 通过主键删除
	 *
	 * @return 影响条数
	 */
	int deleteById(@NotNull Object record);
	
	/**
	 * 删除
	 *
	 * @param filters
	 * @return
	 */
	int deleteByFilters(@Nullable List<Filter> filters);
	
	int deleteByFilter(@NotNull Filter filter);
	
	/**
	 * 是否存在主键
	 *
	 * @return
	 */
	boolean existsId(@NotNull Object record);
	
	@SuppressWarnings("unchecked")
	Class<T> getEntityClass();
	
	/**
	 * 是否存在
	 *
	 * @param columnName
	 * @param value
	 * @return
	 */
	boolean exists(@NotNull String columnName, @NotNull Object value);
	
	boolean existsByFilters(@Nullable List<Filter> filters);
	
	boolean existsByFilter(@NotNull Filter filter);
	
	boolean notExistsId(@NotNull Object record);
	
	boolean notExists(@NotNull String columnName, @NotNull Object value);
	
	boolean notExistsByFilters(@Nullable List<Filter> filters);
	
	boolean notExistsByFilter(@NotNull Filter filter);
	
	/**
	 * 分批消费
	 *
	 * @param filters   过滤
	 * @param batchSize 批次大小
	 * @param orderBy   排序
	 * @param consumer  消费回调
	 */
	void batchConsume(@NotNull List<Filter> filters, int batchSize, @Nullable String orderBy, @NotNull Consumer<List<T>> consumer);
	
	/**
	 * 分组统计
	 *
	 * @param groupField 分组字段
	 * @param filters    过滤条件
	 * @return 统计结果
	 */
	List<GroupCount> findGroupCounts(@NotNull String groupField, @Nullable List<Filter> filters);
	
	<ResultType> ResultType findMax(@NotNull String fieldName, @NotNull List<Filter> filters);
	
	<ResultType> ResultType findMin(@NotNull String fieldName, @NotNull List<Filter> filters);
	
	<ResultType> ResultType findAvg(@NotNull String fieldName, @NotNull List<Filter> filters);
	
	/**
	 * 新增
	 *
	 * @param column 字段名
	 * @param value  值
	 * @return 修改数
	 */
	int add(@NotNull String column, int value, @NotNull List<Filter> filters);
	
	int add(@NotNull String column, int value, @NotNull Filter filter);
}
