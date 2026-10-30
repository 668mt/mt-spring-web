package mt.generator.mybatis.core.scanner;

import mt.common.annotation.ForeignKey;
import mt.common.config.CommonProperties;
import mt.generator.mybatis.annotation.IndexType;
import mt.generator.mybatis.core.model.ColumnDefinition;
import mt.generator.mybatis.core.model.ColumnType;
import mt.generator.mybatis.core.model.ForeignKeyDefinition;
import mt.generator.mybatis.core.model.IndexDefinition;
import mt.generator.mybatis.core.model.TableDefinition;
import mt.generator.mybatis.testentity.Notice;
import mt.generator.mybatis.testentity.Primitive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 实体扫描测试
 *
 * @author Martin
 */
class EntityScannerTest {

	private EntityScanner scanner;

	@BeforeEach
	void setUp() {
		scanner = new EntityScanner(new CommonProperties());
	}

	@Test
	void resolveTableName() {
		assertEquals("notice", scanner.resolveTableName(Notice.class));
		assertEquals("idGenerate", scanner.resolveTableName(mt.common.entity.IdGenerate.class));
		assertEquals("dataLock", scanner.resolveTableName(mt.common.entity.DataLock.class));
	}

	@Test
	void columnsOfEntity() {
		TableDefinition table = scanner.toTableDefinition(Notice.class);
		assertEquals("notice", table.getName());
		assertEquals("id", table.getPrimaryKeyColumns().get(0));

		ColumnDefinition id = findColumn(table, "id");
		assertNotNull(id);
		assertTrue(id.isAutoIncrement());
		//@Column 没写 nullable 时默认允许为 null
		assertTrue(id.isNullable());

		ColumnDefinition title = findColumn(table, "title");
		assertNotNull(title);
		assertFalse(title.isNullable());
		assertEquals(ColumnType.STRING, title.getColumnType());
		//@Column 没写长度时使用 jpa 默认值 255
		assertEquals(255, title.getLength());

		ColumnDefinition content = findColumn(table, "content");
		assertNotNull(content);
		assertEquals("longtext", content.getColumnDefinition());

		ColumnDefinition attachment = findColumn(table, "attachment");
		assertNotNull(attachment);
		assertEquals(ColumnType.BINARY, attachment.getColumnType());

		ColumnDefinition amount = findColumn(table, "amount");
		assertNotNull(amount);
		assertEquals(ColumnType.DECIMAL, amount.getColumnType());

		ColumnDefinition status = findColumn(table, "status");
		assertNotNull(status);
		assertEquals(ColumnType.ENUM, status.getColumnType());
	}

	@Test
	void primitiveTypesAreMappedCorrectly() {
		TableDefinition table = scanner.toTableDefinition(Primitive.class);
		assertEquals(ColumnType.INTEGER, findColumn(table, "intField").getColumnType());
		assertEquals(ColumnType.LONG, findColumn(table, "longField").getColumnType());
		assertEquals(ColumnType.BOOLEAN, findColumn(table, "booleanField").getColumnType());
		assertEquals(ColumnType.DATE, findColumn(table, "localDateField").getColumnType());
		assertEquals(ColumnType.TIMESTAMP, findColumn(table, "localDateTimeField").getColumnType());
	}

	@Test
	void indexesOfEntity() {
		TableDefinition table = scanner.toTableDefinition(Notice.class);
		List<IndexDefinition> indexes = table.getIndexes();
		assertEquals(2, indexes.size());
		IndexDefinition first = indexes.get(0);
		assertEquals(IndexType.normal, first.getType());
		assertEquals("noticeType", first.getColumns().get(0));
		IndexDefinition second = indexes.get(1);
		assertEquals("idx_notice_user", second.getName());
		assertEquals(2, second.getColumns().size());
	}

	@Test
	void foreignKeyOfEntity() {
		TableDefinition table = scanner.toTableDefinition(Notice.NoticeDetail.class);
		assertEquals(1, table.getForeignKeys().size());
		ForeignKeyDefinition foreignKey = table.getForeignKeys().get(0);
		assertEquals("notice_detail", foreignKey.getTableName());
		assertEquals("notice_id", foreignKey.getColumnName());
		assertEquals("notice", foreignKey.getReferenceTableName());
		assertEquals("id", foreignKey.getReferencedColumnName());
		assertEquals(ForeignKey.CascadeType.DELETE, foreignKey.getCascadeType());
	}

	@Test
	void scanPackages() {
		List<Class<?>> entities = scanner.scan(new String[]{"mt.generator.mybatis.testentity"});
		assertTrue(entities.contains(Notice.class));
		assertTrue(entities.contains(Primitive.class));
	}

	private ColumnDefinition findColumn(TableDefinition table, String name) {
		return table.getColumns().stream().filter(c -> name.equals(c.getName())).findFirst().orElse(null);
	}
}
