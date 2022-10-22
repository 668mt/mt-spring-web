package mt.spring.tools.base;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class FileSizeUtils {
	/**
	 * 将文件大小转换为可读的，如10MB,1GB
	 *
	 * @param sizeByte 文件大小
	 * @return 转换结果
	 */
	public static String getReadableSize(long sizeByte) {
		return getReadableSize(sizeByte, "#.###");
	}
	
	/**
	 * 将文件大小转换为可读的，如10MB,1GB
	 *
	 * @param sizeByte 文件大小
	 * @param pattern  格式，如#.### 或 0.00
	 * @return 转换结果
	 */
	public static String getReadableSize(long sizeByte, @NotNull String pattern) {
		BigDecimal size;
		String unit;
		if (sizeByte < 0) {
			return null;
		} else if (sizeByte <= 1024) {
			size = BigDecimal.valueOf(sizeByte);
			unit = "B";
		} else if (sizeByte <= 1024 * 1024) {
			size = BigDecimal.valueOf(sizeByte).divide(BigDecimal.valueOf(1024), 3, RoundingMode.HALF_UP);
			unit = "KB";
		} else if (sizeByte <= 1024 * 1024 * 1024) {
			size = BigDecimal.valueOf(sizeByte).divide(BigDecimal.valueOf(1024 * 1024), 3, RoundingMode.HALF_UP);
			unit = "MB";
		} else {
			size = BigDecimal.valueOf(sizeByte).divide(BigDecimal.valueOf(1024 * 1024 * 1024), 3, RoundingMode.HALF_UP);
			unit = "GB";
		}
		return new DecimalFormat(pattern).format(size) + unit;
	}
}