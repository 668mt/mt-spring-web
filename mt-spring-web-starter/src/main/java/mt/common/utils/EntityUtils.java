package mt.common.utils;

import lombok.SneakyThrows;
import mt.common.tkmapper.Filter;
import mt.utils.ReflectUtils;
import mt.utils.common.Assert;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2024/4/27
 */
public class EntityUtils {
	@SneakyThrows
	@NotNull
	public static List<Filter> getIdFilters(@NotNull Class<?> entityClass, @NotNull Object record) {
		List<mt.common.tkmapper.Filter> filters = new ArrayList<>();
		List<Field> fields = ReflectUtils.findAllFields(entityClass, Id.class);
		boolean isEntity = entityClass.isAssignableFrom(record.getClass());
		Assert.notEmpty(fields, "未找到@Id字段");
		if (!isEntity && fields.size() > 1) {
			throw new IllegalArgumentException("多个@Id字段，但传入的record不是实体类");
		}
		for (Field field : fields) {
			Object value;
			if (isEntity) {
				field.setAccessible(true);
				value = field.get(record);
			} else {
				value = record;
			}
			String fieldName = field.getName();
			Assert.notNull(value, "Id字段值不能为空:" + fieldName);
			filters.add(new mt.common.tkmapper.Filter(fieldName, mt.common.tkmapper.Filter.Operator.eq, value));
		}
		return filters;
	}
}
