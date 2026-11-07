package mt.generator.mybatis;

import mt.common.mybatis.event.AfterInitEvent;
import mt.generator.mybatis.status.TableInitResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 建表结果查询的测试。
 *
 * @author Martin
 */
class GeneratorStatusTest {

	@Test
	void firstInitWhenAllTablesAreNew() {
		AfterInitEvent event = new AfterInitEvent(this);
		event.setCreateTable(true);
		event.setNewTables(Arrays.asList("notice", "notice_detail"));

		Generator.record(event, Collections.emptyList());

		TableInitResult result = Generator.getInitResult();
		assertTrue(result.isInitialized());
		assertTrue(result.isTableCreated());
		assertFalse(result.isDatabaseCreated());
		//一张旧表都没有，属于第一次初始化
		assertTrue(result.isFirstInit());
		assertEquals(2, result.getNewTables().size());
		assertTrue(result.getExistingTables().isEmpty());
	}

	@Test
	void notFirstInitWhenSomeTablesAlreadyExist() {
		AfterInitEvent event = new AfterInitEvent(this);
		event.setCreateTable(true);
		event.setNewTables(Collections.singletonList("notice"));

		Generator.record(event, Collections.singletonList("dataLock"));

		TableInitResult result = Generator.getInitResult();
		assertTrue(result.isTableCreated());
		//已经有旧表了，不是第一次初始化
		assertFalse(result.isFirstInit());
		assertEquals(Collections.singletonList("dataLock"), result.getExistingTables());
	}

	@Test
	void firstInitWhenDatabaseIsCreated() {
		AfterInitEvent event = new AfterInitEvent(this);
		event.setCreateDatabase(true);
		event.setCreateTable(true);
		event.setNewTables(Collections.singletonList("notice"));

		Generator.record(event, Collections.emptyList());

		TableInitResult result = Generator.getInitResult();
		assertTrue(result.isDatabaseCreated());
		assertTrue(result.isFirstInit());
	}

	@Test
	void nothingCreatedWhenAllTablesAlreadyExist() {
		AfterInitEvent event = new AfterInitEvent(this);
		event.setCreateTable(false);

		Generator.record(event, Arrays.asList("notice", "dataLock"));

		TableInitResult result = Generator.getInitResult();
		assertTrue(result.isInitialized());
		assertFalse(result.isTableCreated());
		assertFalse(result.isFirstInit());
		assertTrue(result.getNewTables().isEmpty());
	}

	@Test
	void emptyResultWhenGeneratorNeverRan() {
		TableInitResult result = TableInitResult.empty();
		assertFalse(result.isInitialized());
		assertFalse(result.isTableCreated());
		assertFalse(result.isFirstInit());
		assertTrue(result.getNewTables().isEmpty());
		assertTrue(result.getExistingTables().isEmpty());
	}
}
