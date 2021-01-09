package mt.utils.common;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
public class MathUtils {
	public static String toFixed(String number, int len) {
		return toFixed(number, len, false);
	}
	
	public static String toFixed(String number, int len, boolean blankSetZero) {
		double value;
		if (StringUtils.isBlank(number)) {
			if (blankSetZero) {
				value = 0;
			} else {
				return "";
			}
		} else {
			value = new BigDecimal(number).setScale(len, RoundingMode.HALF_UP).doubleValue();
		}
		StringBuilder pattern = new StringBuilder("#");
		if (len > 0) {
			pattern.append(".");
			for (int i = 0; i < len; i++) {
				pattern.append("0");
			}
		}
		return new DecimalFormat(pattern.toString()).format(value);
	}
}
