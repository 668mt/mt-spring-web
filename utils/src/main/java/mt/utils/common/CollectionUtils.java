package mt.utils.common;

import mt.utils.ReflectUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
public class CollectionUtils {
	public static boolean isEmpty(@Nullable Collection<?> collection) {
		return collection == null || collection.size() == 0;
	}
	
	public static boolean isNotEmpty(@Nullable Collection<?> collection) {
		return !isEmpty(collection);
	}
	
	
	/**
	 * 进行List分组
	 *
	 * @param list 需要分组的列表
	 * @param key  按指定字段进行分组
	 * @param type 指定字段的数据类型
	 * @return
	 */
	public static <T, T2> Map<T2, List<T>> groupBy(List<T> list, String key, Class<T2> type) {
		Map<T2, List<T>> map = new HashMap<>();
		for (T obj : list) {
			T2 value = ReflectUtils.getValue(obj, key, type);
			List<T> list2 = map.get(value);
			if (isEmpty(list2)) {
				list2 = new ArrayList<T>();
				map.put(value, list2);
			}
			list2.add(obj);
		}
		return map;
	}
	
	/**
	 * 按字符串字段分组
	 *
	 * @param list 待分组的List
	 * @param key  按指定字段进行分组
	 * @return
	 */
	public static <T> Map<String, List<T>> groupBy(List<T> list, String key) {
		return groupBy(list, key, String.class);
	}
	
}
