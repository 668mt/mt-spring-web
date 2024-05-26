package mt.common.starter.message.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mt.common.starter.message.messagehandler.BatchMessageHandler;

import java.util.Objects;

/**
 * @Author Martin
 * @Date 2024/5/26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchMessageKey {
	private Class<? extends BatchMessageHandler<?, ?>> handlerClass;
	private String[] params;
	
	public int hashCode() {
		String paramsString = params == null ? "" : String.join(",", params);
		return Objects.hash(handlerClass, paramsString);
	}
	
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof BatchMessageKey other) {
			String paramsString = params == null ? "" : String.join(",", params);
			String otherParamsString = other.params == null ? "" : String.join(",", other.params);
			return Objects.equals(handlerClass, other.handlerClass) && Objects.equals(paramsString, otherParamsString);
		} else {
			return false;
		}
	}
}
