package mt.generator.mybatis.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自动建表的结果。
 * <p>
 * 建表在启动时执行一次，结果记在这个对象里，通过 {@code Generator.getInitResult()} 拿到。
 *
 * @author Martin
 */
public class TableInitResult {

	/**
	 * 建表流程是否执行过
	 */
	private final boolean initialized;

	/**
	 * 本次是否新建了数据库
	 */
	private final boolean databaseCreated;

	/**
	 * 本次是否新建了表
	 */
	private final boolean tableCreated;

	/**
	 * 是否第一次初始化
	 */
	private final boolean firstInit;

	/**
	 * 本次新建的表
	 */
	private final List<String> newTables;

	/**
	 * 本次之前就已经存在的表
	 */
	private final List<String> existingTables;

	/**
	 * 本次新增的字段
	 */
	private final List<String> newColumns;

	/**
	 * 本次新建的索引
	 */
	private final List<String> newIndexes;

	/**
	 * 本次新建的外键
	 */
	private final List<String> newForeightKeys;

	public TableInitResult(boolean databaseCreated, boolean tableCreated, List<String> newTables,
							List<String> existingTables, List<String> newColumns, List<String> newIndexes,
							List<String> newForeightKeys) {
		this(true, databaseCreated, tableCreated, newTables, existingTables, newColumns, newIndexes, newForeightKeys);
	}

	private TableInitResult(boolean initialized, boolean databaseCreated, boolean tableCreated, List<String> newTables,
							 List<String> existingTables, List<String> newColumns, List<String> newIndexes,
							 List<String> newForeightKeys) {
		this.initialized = initialized;
		this.databaseCreated = databaseCreated;
		this.tableCreated = tableCreated;
		this.newTables = copy(newTables);
		this.existingTables = copy(existingTables);
		this.newColumns = copy(newColumns);
		this.newIndexes = copy(newIndexes);
		this.newForeightKeys = copy(newForeightKeys);
		//新建了数据库，或者本次之前一张表都没有，算第一次初始化
		this.firstInit = databaseCreated || (this.existingTables.isEmpty() && !this.newTables.isEmpty());
	}

	/**
	 * 空的建表结果，表示建表流程没有执行过
	 *
	 * @return 空结果
	 */
	public static TableInitResult empty() {
		return new TableInitResult(false, false, false, Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * 建表流程是否执行过
	 * <p>
	 * 自动建表开关关闭、或者建表过程中直接失败时，这里是 false
	 *
	 * @return 是否执行过
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * 本次是否新建了数据库
	 *
	 * @return 是否新建了数据库
	 */
	public boolean isDatabaseCreated() {
		return databaseCreated;
	}

	/**
	 * 本次是否新建了表
	 *
	 * @return 是否新建了表
	 */
	public boolean isTableCreated() {
		return tableCreated;
	}

	/**
	 * 是否第一次初始化
	 * <p>
	 * 新建了数据库，或者本次之前一张表都没有（所有表都是本次新建的），算第一次初始化。
	 * 可以用来判断要不要插入初始数据。
	 *
	 * @return 是否第一次初始化
	 */
	public boolean isFirstInit() {
		return firstInit;
	}

	/**
	 * 本次新建的表
	 *
	 * @return 表名列表
	 */
	public List<String> getNewTables() {
		return newTables;
	}

	/**
	 * 本次之前就已经存在的表
	 *
	 * @return 表名列表
	 */
	public List<String> getExistingTables() {
		return existingTables;
	}

	/**
	 * 本次新增的字段
	 *
	 * @return 字段名列表
	 */
	public List<String> getNewColumns() {
		return newColumns;
	}

	/**
	 * 本次新建的索引
	 *
	 * @return 索引名列表
	 */
	public List<String> getNewIndexes() {
		return newIndexes;
	}

	/**
	 * 本次新建的外键
	 *
	 * @return 外键名列表
	 */
	public List<String> getNewForeightKeys() {
		return newForeightKeys;
	}

	private static List<String> copy(List<String> source) {
		if (source == null || source.isEmpty()) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(new ArrayList<>(source));
	}
}
