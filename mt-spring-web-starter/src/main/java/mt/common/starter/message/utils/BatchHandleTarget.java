package mt.common.starter.message.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Field;

/**
 * @Author Martin
 * @Date 2024/5/26
 */
@Data
@AllArgsConstructor
public class BatchHandleTarget {
	private Field field;
	private Object target;
	private Object fromFieldValue;
}
