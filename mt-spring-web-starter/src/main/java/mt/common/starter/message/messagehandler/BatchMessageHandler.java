package mt.common.starter.message.messagehandler;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @Author Martin
 * @Date 2019/1/6
 */
public interface BatchMessageHandler<FieldType, ResultType> extends MessageHandler<Object, FieldType> {
	
	Map<FieldType, ResultType> handle(Collection<?> collection, Set<FieldType> fieldValues, String[] params);
	
	@Override
	default FieldType handle(Object o, Object[] params, String mark) {
		throw new IllegalStateException("NOT SUPPORT");
	}
}
