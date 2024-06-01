package mt.common.context;

import lombok.Data;

@Data
public class FieldInfo {
	/**
	 * 字段名称
	 */
	private String fieldName;
	/**
	 * 使用字段
	 */
	private String useField;
}