package mt.utils.common;

import java.lang.reflect.InvocationTargetException;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
public class BeanUtils {
	public static <T1, T2> T2 transformOf(T1 src, Class<T2> type) {
		try {
			T2 dest = type.newInstance();
			org.apache.commons.beanutils.BeanUtils.copyProperties(dest, src);
			return dest;
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
