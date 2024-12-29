package mt.common.mybatis.advanced;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ValueConverter {
	<T> T convert(@Nullable Object value, @NotNull Class<T> targetType);
}