package mt.common.mybatis.event;

import org.springframework.context.ApplicationEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 初始化事件
* @ClassName: InitEvent
* @Description: 
* @author Martin
* @date 2017-10-24 上午11:28:59
*
 */
public class AfterInitEvent extends ApplicationEvent{

	private static final long serialVersionUID = 4695186067723946564L;
	
	/**
	 * 是否创建数据库
	 */
	private boolean isCreateDatabase;
	/**
	 * 是否创建表
	 */
	private boolean isCreateTable;
	/**
	 * 是否创建主外键
	 */
	private boolean isCreateForeightKey;
	/**
	 * 创建的新表
	 */
	private List<String> newTables = new ArrayList<String>();
	private List<String> newForeightKeys = new ArrayList<String>();
	/**
	 * 创建的新索引
	 */
	private List<String> newIndexes = new ArrayList<String>();
	/**
	 * 新增的字段
	 */
	private List<String> newColumns = new ArrayList<String>();
	
	public List<String> getNewForeightKeys() {
		return newForeightKeys;
	}

	public void setNewForeightKeys(List<String> newForeightKeys) {
		this.newForeightKeys = newForeightKeys;
	}

	public List<String> getNewIndexes() {
		return newIndexes;
	}

	public void setNewIndexes(List<String> newIndexes) {
		this.newIndexes = newIndexes;
	}

	public List<String> getNewColumns() {
		return newColumns;
	}

	public void setNewColumns(List<String> newColumns) {
		this.newColumns = newColumns;
	}

	public boolean isCreateDatabase() {
		return isCreateDatabase;
	}

	public void setCreateDatabase(boolean isCreateDatabase) {
		this.isCreateDatabase = isCreateDatabase;
	}

	public boolean isCreateTable() {
		return isCreateTable;
	}

	public List<String> getNewTables() {
		return newTables;
	}

	public void setNewTables(List<String> newTables) {
		this.newTables = newTables;
	}

	public void setCreateTable(boolean isCreateTable) {
		this.isCreateTable = isCreateTable;
	}

	public boolean isCreateForeightKey() {
		return isCreateForeightKey;
	}

	public void setCreateForeightKey(boolean isCreateForeightKey) {
		this.isCreateForeightKey = isCreateForeightKey;
	}

	public AfterInitEvent(Object source, boolean isCreateDatabase, boolean isCreateTable, boolean isCreateForeightKey) {
		super(source);
		this.isCreateDatabase = isCreateDatabase;
		this.isCreateTable = isCreateTable;
		this.isCreateForeightKey = isCreateForeightKey;
	}

	public AfterInitEvent(Object source) {
		super(source);
	}

}
