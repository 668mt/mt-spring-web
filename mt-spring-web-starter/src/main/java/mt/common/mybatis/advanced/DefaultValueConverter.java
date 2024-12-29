package mt.common.mybatis.advanced;

import lombok.SneakyThrows;
import org.apache.commons.beanutils.ConvertUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @Author Martin
 * @Date 2024/12/29
 */
public class DefaultValueConverter implements ValueConverter {
	@SneakyThrows
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public <T> T convert(@Nullable Object value, @NotNull Class<T> targetType) {
		if (value == null) {
			return null;
		}
		if (targetType.isEnum()) {
			return (T) Enum.valueOf((Class<? extends Enum>) targetType, value.toString());
		}
		return (T) ConvertUtils.convert(value, targetType);
	}
}
