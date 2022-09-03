package mt.common.config;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author Martin
 * @Date 2022/8/13
 */
public class SystemEntity {
	private static final Set<Class<?>> entities = new HashSet<>();
	
	public static void register(@NotNull Class<?> entityClass) {
		entities.add(entityClass);
	}
	
	public static Set<Class<?>> getEntities() {
		return entities;
	}
}
