package mt.generator.mybatis.testentity;

import lombok.Data;
import mt.common.entity.po.BaseEntity;

import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 测试基本类型映射的实体
 *
 * @author Martin
 */
@Data
@Table(name = "primitive")
public class Primitive extends BaseEntity {

	private int intField;

	private long longField;

	private boolean booleanField;

	private Integer integerField;

	private Long longObjectField;

	private Boolean booleanObjectField;

	private double doubleField;

	private float floatField;

	private short shortField;

	private byte byteField;

	private LocalDate localDateField;

	private LocalDateTime localDateTimeField;
}
