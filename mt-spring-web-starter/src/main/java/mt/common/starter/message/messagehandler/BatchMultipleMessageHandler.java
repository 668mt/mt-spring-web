package mt.common.starter.message.messagehandler;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @Author Martin
 * @Date 2019/1/6
 */
public interface BatchMultipleMessageHandler<ResultType> extends BatchMessageHandler<MultipleFieldValue, ResultType> {
	
	Map<MultipleFieldValue, ResultType> handle(Collection<?> collection, Set<MultipleFieldValue> values, String[] params);
	
}
