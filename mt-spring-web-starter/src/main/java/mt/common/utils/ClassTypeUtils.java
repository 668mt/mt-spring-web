package mt.common.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @Author Martin
 * @Date 2021/8/29
 */
public class ClassTypeUtils {
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static ParameterizedType findGenericInterface(Class clazz, Class interClass) {
		Type[] genericInterfaces = clazz.getGenericInterfaces();
		if (genericInterfaces.length > 0) {
			for (Type genericInterface : genericInterfaces) {
				if (!(genericInterface instanceof ParameterizedType)) {
					continue;
				}
				ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
				Type rawType = parameterizedType.getRawType();
				if (interClass.isAssignableFrom((Class<?>) rawType)) {
					return parameterizedType;
				}
			}
		}
		Class superclass = clazz.getSuperclass();
		if (superclass.equals(Object.class)) {
			return null;
		}
		return findGenericInterface(superclass, interClass);
	}
}
