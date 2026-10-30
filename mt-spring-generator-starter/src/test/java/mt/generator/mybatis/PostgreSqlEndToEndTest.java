package mt.generator.mybatis;

import mt.common.config.CommonProperties;
import mt.generator.mybatis.core.dialect.Dialect;
import mt.generator.mybatis.core.dialect.PostgreSqlDialect;
import mt.generator.mybatis.core.meta.SchemaInspector;
import mt.generator.mybatis.core.scanner.EntityScanner;
import mt.generator.mybatis.utils.GenerateHelper;
import mt.generator.mybatis.utils.Jdbc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实 PostgreSQL 上的端到端测试。
 * <p>
 * 需要通过环境变量指定数据库连接，未配置时跳过：
 * <pre>
 * MT_GENERATOR_PG_URL=jdbc:postgresql://127.0.0.1:5432/mt_gen_test
 * MT_GENERATOR_PG_USER=postgres
 * MT_GENERATOR_PG_PASSWORD=postgres
 * </pre>
 *
 * @author Martin
 */
@EnabledIfEnvironmentVariable(named = "MT_GENERATOR_PG_URL", matches = ".+")
class PostgreSqlEndToEndTest {

	private static final String TEST_DB = "mt_gen_test";

	private static final String ENTITY_PACKAGE = "mt.generator.mybatis.testentity";

	private String jdbcUrl;

	private String user;

	private String password;

	@BeforeEach
	void setUp() {
		jdbcUrl = System.getenv("MT_GENERATOR_PG_URL");
		user = System.getenv("MT_GENERATOR_PG_USER");
		password = System.getenv("MT_GENERATOR_PG_PASSWORD");
		dropTestDatabase();
	}

	@AfterEach
	void tearDown() {
		dropTestDatabase();
	}

	@Test
	void generateSchemaOnRealPostgres() throws Exception {
		CommonProperties commonProperties = new CommonProperties();
		Dialect dialect = new PostgreSqlDialect();
		GenerateHelper helper = new GenerateHelper(jdbcUrl, "org.postgresql.Driver", user, password, dialect,
				commonProperties);
		helper.init(new String[]{ENTITY_PACKAGE}, new mt.common.mybatis.event.AfterInitEvent(helper));

		Jdbc jdbc = new Jdbc("org.postgresql.Driver", jdbcUrl, user, password);
		SchemaInspector inspector = new SchemaInspector(dialect);

		try (Connection connection = jdbc.getConnection()) {
			assertTrue(inspector.databaseExists(connection, TEST_DB), "数据库应该被自动创建");

			//表都建好了
			assertNotNull(inspector.findTable(connection, TEST_DB, "notice"));
			assertNotNull(inspector.findTable(connection, TEST_DB, "notice_detail"));
			assertNotNull(inspector.findTable(connection, TEST_DB, "primitive"));

			//字段
			Set<String> columns = inspector.listColumns(connection, TEST_DB, "notice");
			assertTrue(columns.contains("userid"), "字段列表：" + columns);
			assertTrue(columns.contains("istop"), "字段列表：" + columns);
			assertTrue(columns.contains("attachment"), "字段列表：" + columns);

			//索引
			List<String> indexes = query(connection,
					"SELECT indexname FROM pg_indexes WHERE tablename = 'notice' ORDER BY indexname");
			assertTrue(indexes.contains("idx_notice_user"), "索引列表：" + indexes);
			assertTrue(indexes.contains("index_notice_noticetype"), "索引列表：" + indexes);

			//外键
			List<String> constraints = query(connection,
					"SELECT conname FROM pg_constraint WHERE conrelid = 'notice_detail'::regclass");
			assertTrue(constraints.contains("fk_notice_detail_notice_id_notice_id"), "约束列表：" + constraints);
		}

		//自增主键可以用
		try (Connection connection = jdbc.getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO notice (title, istop, noticetype) VALUES ('t', true, 1)");
			try (ResultSet rs = statement.executeQuery("SELECT count(*) FROM notice")) {
				rs.next();
				assertEquals(1, rs.getInt(1));
			}
		}

		//再跑一遍不能报错，也不能重复建表
		GenerateHelper again = new GenerateHelper(jdbcUrl, "org.postgresql.Driver", user, password, dialect,
				commonProperties);
		again.init(new String[]{ENTITY_PACKAGE}, new mt.common.mybatis.event.AfterInitEvent(again));
	}

	@Test
	void addMissingColumn() throws Exception {
		CommonProperties commonProperties = new CommonProperties();
		commonProperties.getGenerator().setAddMissingColumn(true);
		Dialect dialect = new PostgreSqlDialect();

		GenerateHelper helper = new GenerateHelper(jdbcUrl, "org.postgresql.Driver", user, password, dialect,
				commonProperties);
		helper.init(new String[]{ENTITY_PACKAGE}, new mt.common.mybatis.event.AfterInitEvent(helper));

		//模拟老版本建出来的表，字段比实体少
		Jdbc jdbc = new Jdbc("org.postgresql.Driver", jdbcUrl, user, password);
		try (Connection connection = jdbc.getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("ALTER TABLE notice DROP COLUMN userid");
		}

		GenerateHelper again = new GenerateHelper(jdbcUrl, "org.postgresql.Driver", user, password, dialect,
				commonProperties);
		again.init(new String[]{ENTITY_PACKAGE}, new mt.common.mybatis.event.AfterInitEvent(again));

		SchemaInspector inspector = new SchemaInspector(dialect);
		try (Connection connection = jdbc.getConnection()) {
			Set<String> columns = inspector.listColumns(connection, TEST_DB, "notice");
			assertTrue(columns.contains("userid"), "缺失的字段应该被补齐：" + columns);
		}
	}

	@Test
	void createDatabaseWhenNotExists() throws Exception {
		//库不存在时，生成器要自己把库建出来
		Jdbc maintenance = new Jdbc("org.postgresql.Driver", jdbcUrl.replace("/" + TEST_DB, "/postgres"), user, password);
		try (Connection connection = maintenance.getConnection()) {
			assertFalse(new SchemaInspector(new PostgreSqlDialect()).databaseExists(connection, TEST_DB));
		}
		CommonProperties commonProperties = new CommonProperties();
		GenerateHelper helper = new GenerateHelper(jdbcUrl, "org.postgresql.Driver", user, password,
				new PostgreSqlDialect(), commonProperties);
		helper.init(new String[]{ENTITY_PACKAGE}, new mt.common.mybatis.event.AfterInitEvent(helper));
		try (Connection connection = maintenance.getConnection()) {
			assertTrue(new SchemaInspector(new PostgreSqlDialect()).databaseExists(connection, TEST_DB));
		}
	}

	@Test
	void entityScannerFindsAllEntities() {
		EntityScanner scanner = new EntityScanner(new CommonProperties());
		List<Class<?>> entities = scanner.scan(new String[]{ENTITY_PACKAGE});
		assertEquals(3, entities.size(), "实体列表：" + entities);
	}

	/**
	 * 框架自带的 idGenerate / dataLock 表，运行时用的是拼出来的原生 SQL，
	 * 这里验证 PostgreSQL 折叠成小写之后依然能对上
	 */
	@Test
	void frameworkTablesWorkWithNativeSql() throws Exception {
		mt.common.config.SystemEntity.register(mt.common.entity.IdGenerate.class);
		mt.common.config.SystemEntity.register(mt.common.entity.DataLock.class);
		CommonProperties commonProperties = new CommonProperties();
		Dialect dialect = new PostgreSqlDialect();
		GenerateHelper helper = new GenerateHelper(jdbcUrl, "org.postgresql.Driver", user, password, dialect,
				commonProperties);
		helper.init(new String[]{ENTITY_PACKAGE}, new mt.common.mybatis.event.AfterInitEvent(helper));

		Jdbc jdbc = new Jdbc("org.postgresql.Driver", jdbcUrl, user, password);
		try (Connection connection = jdbc.getConnection()) {
			SchemaInspector inspector = new SchemaInspector(dialect);
			assertNotNull(inspector.findTable(connection, TEST_DB, "idGenerate"));
			assertNotNull(inspector.findTable(connection, TEST_DB, "dataLock"));
		}

		//IdGenerateService / DataLockService 里拼的原生 SQL
		try (Connection connection = jdbc.getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO " + commonProperties.getIdGenerateTableName()
					+ " (tableName,nextValue) VALUES ('notice', 2)");
			statement.executeUpdate("INSERT INTO " + commonProperties.getDataLockTableName()
					+ " (id,useKey) VALUES ('idGenerate','init')");
		}
		try (Connection connection = jdbc.getConnection(); Statement statement = connection.createStatement();
			 ResultSet rs = statement.executeQuery("SELECT tableName,nextValue FROM "
						+ commonProperties.getIdGenerateTableName())) {
			assertTrue(rs.next());
			assertEquals("notice", rs.getString(1));
			assertEquals(2, rs.getLong(2));
		}
	}

	private void dropTestDatabase() {
		String url = jdbcUrl.replace("/" + TEST_DB, "/postgres");
		Jdbc jdbc = new Jdbc("org.postgresql.Driver", url, user, password);
		try (Connection connection = jdbc.getConnection(); Statement statement = connection.createStatement()) {
			statement.execute("DROP DATABASE IF EXISTS " + TEST_DB + " WITH (FORCE)");
		} catch (Exception e) {
			// 清理失败不影响测试结果
			System.out.println("清理测试库失败：" + e.getMessage());
		}
	}

	private List<String> query(Connection connection, String sql) throws Exception {
		List<String> list = new java.util.ArrayList<>();
		try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
			while (rs.next()) {
				list.add(rs.getString(1));
			}
		}
		return list;
	}

	private void assertNotNull(Object object) {
		assertTrue(object != null, "不应该为 null");
	}
}
