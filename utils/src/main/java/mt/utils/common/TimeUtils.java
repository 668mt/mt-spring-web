package mt.utils.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
public class TimeUtils {
	public static final Long ONE_SECOND = 1000L;
	public static final Long ONE_MINUTE = 60 * ONE_SECOND;
	public static final Long ONE_HOUR = 60 * ONE_MINUTE;
	public static final Long ONE_DAY = 24 * ONE_HOUR;
	
	public static String getReadableTime(long millSeconds) {
		millSeconds = Math.abs(millSeconds);
		long days = millSeconds / ONE_DAY;
		millSeconds = millSeconds % ONE_DAY;
		long hours = millSeconds / ONE_HOUR;
		millSeconds = millSeconds % ONE_HOUR;
		long mins = millSeconds / ONE_MINUTE;
		millSeconds = millSeconds % ONE_MINUTE;
		long secs = millSeconds / ONE_SECOND;
		return String.format("%d天%d时%d分%d秒", days, hours, mins, secs);
	}
	
	public static String getReadableTime(long millSeconds, int scale) {
		long mills = Math.abs(millSeconds);
		long x;
		String unit;
		if (mills < 1000) {
			x = 1;
			unit = "毫秒";
		} else if (mills < 1000 * 60) {
			x = 1000;
			unit = "秒";
		} else if (mills < 1000 * 60 * 60) {
			x = 1000 * 60;
			unit = "分钟";
		} else if (mills < 1000 * 60 * 60 * 24) {
			x = 1000 * 60 * 60;
			unit = "小时";
		} else {
			x = 1000 * 60 * 60 * 24;
			unit = "天";
		}
		double readableNum = BigDecimal.valueOf(mills).divide(BigDecimal.valueOf(x), scale, RoundingMode.HALF_UP).doubleValue();
		String prefix = millSeconds < 0 ? "-" : "";
		return prefix + readableNum + unit;
	}
}
