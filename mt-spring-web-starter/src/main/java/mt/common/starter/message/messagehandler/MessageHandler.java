package mt.common.starter.message.messagehandler;

import org.apache.commons.beanutils.ConvertUtils;

/**
 * @Author Martin
 * @Date 2019/1/6
 */
public interface MessageHandler<Entity, FieldType> {
	
	@SuppressWarnings("unchecked")
	default <T> T getParam(Object[] params, int index, Class<T> type) {
		if (params == null) {
			return null;
		}
		if (index + 1 > params.length) {
			return null;
		}
		Object param = params[index];
		if (param == null) {
			return null;
		}
		return (T) ConvertUtils.convert(param, type);
	}
	
	/**
	 * 此方法在message对象前触发
	 */
	default void init() {
		//初始化
	}
	
	FieldType handle(Entity entity, Object[] params, String mark);
}
