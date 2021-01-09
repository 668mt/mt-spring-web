package mt.utils.common;

/**
 * @author Martin
 * @ClassName: MyUtils
 * @date 2017-10-13 下午3:38:52
 */
public class ObjectUtils {
	
	@SafeVarargs
	public static <T> T nullAsDefault(T value, T... defaultValues) {
		if (value != null) {
			return value;
		}
		if (defaultValues == null) {
			return null;
		}
		for (T arg : defaultValues) {
			if (arg != null) {
				return arg;
			}
		}
		return null;
	}
}
