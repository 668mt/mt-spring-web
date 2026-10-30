package mt.generator.mybatis.core.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Java 类型解析测试
 *
 * @author Martin
 */
class JavaTypeResolverTest {

	@Test
	void primitiveTypesAreNotStrings() {
		//原始实现用的是 type.isAssignableFrom(X.class)，方向反了，基本类型全部退化成 varchar
		assertEquals(ColumnType.INTEGER, JavaTypeResolver.resolve(int.class));
		assertEquals(ColumnType.LONG, JavaTypeResolver.resolve(long.class));
		assertEquals(ColumnType.BOOLEAN, JavaTypeResolver.resolve(boolean.class));
		assertEquals(ColumnType.SHORT, JavaTypeResolver.resolve(short.class));
		assertEquals(ColumnType.BYTE, JavaTypeResolver.resolve(byte.class));
		assertEquals(ColumnType.DOUBLE, JavaTypeResolver.resolve(double.class));
		assertEquals(ColumnType.FLOAT, JavaTypeResolver.resolve(float.class));
	}

	@Test
	void wrapperTypes() {
		assertEquals(ColumnType.INTEGER, JavaTypeResolver.resolve(Integer.class));
		assertEquals(ColumnType.LONG, JavaTypeResolver.resolve(Long.class));
		assertEquals(ColumnType.BOOLEAN, JavaTypeResolver.resolve(Boolean.class));
		assertEquals(ColumnType.DECIMAL, JavaTypeResolver.resolve(BigDecimal.class));
	}

	@Test
	void dateTypes() {
		assertEquals(ColumnType.TIMESTAMP, JavaTypeResolver.resolve(Date.class));
		assertEquals(ColumnType.TIMESTAMP, JavaTypeResolver.resolve(java.sql.Timestamp.class));
		assertEquals(ColumnType.DATE, JavaTypeResolver.resolve(java.sql.Date.class));
		assertEquals(ColumnType.TIME, JavaTypeResolver.resolve(java.sql.Time.class));
		assertEquals(ColumnType.DATE, JavaTypeResolver.resolve(LocalDate.class));
		assertEquals(ColumnType.TIMESTAMP, JavaTypeResolver.resolve(LocalDateTime.class));
		assertEquals(ColumnType.TIMESTAMP, JavaTypeResolver.resolve(Instant.class));
	}

	@Test
	void stringAndEnum() {
		assertEquals(ColumnType.STRING, JavaTypeResolver.resolve(String.class));
		assertEquals(ColumnType.STRING, JavaTypeResolver.resolve(char.class));
		assertEquals(ColumnType.ENUM, JavaTypeResolver.resolve(Thread.State.class));
	}

	@Test
	void binary() {
		assertEquals(ColumnType.BINARY, JavaTypeResolver.resolve(byte[].class));
	}
}
