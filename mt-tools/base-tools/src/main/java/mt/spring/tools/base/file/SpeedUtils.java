package mt.spring.tools.base.file;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2023/8/19
 */
public class SpeedUtils {
	public static String getSpeed(long length, long costMills) {
		long seconds = TimeUnit.MILLISECONDS.toSeconds(costMills);
		if (seconds <= 0) {
			seconds = 1;
		}
		long speed = BigDecimal.valueOf(length).divide(BigDecimal.valueOf(seconds), 0, RoundingMode.HALF_UP).longValue();
		return SizeUtils.getReadableSize(speed) + "/s";
	}
}
