package mt.utils.common;

import org.jetbrains.annotations.Nullable;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
public class ArrayUtils {
	public static boolean isEmpty(@Nullable Object[] array) {
		return array == null || array.length == 0;
	}
	
	public static boolean isNotEmpty(@Nullable Object[] array) {
		return !isEmpty(array);
	}
}
