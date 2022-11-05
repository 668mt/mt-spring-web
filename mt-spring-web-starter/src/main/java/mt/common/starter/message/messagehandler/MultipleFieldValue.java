package mt.common.starter.message.messagehandler;

import lombok.Data;

import java.util.Arrays;

/**
 * @Author Martin
 * @Date 2022/10/31
 */
@Data
public class MultipleFieldValue {
	private Object[] values;
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}
	
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof MultipleFieldValue) {
			MultipleFieldValue target = (MultipleFieldValue) obj;
			return Arrays.equals(values, target.values);
		}
		return false;
	}
	
}
