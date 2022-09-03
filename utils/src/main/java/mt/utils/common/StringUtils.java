package mt.utils.common;

import org.jetbrains.annotations.Nullable;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
public class StringUtils {
	/**
	 * 特殊字符正则表达式
	 */
	public static final String SPECIAL_CHAR_REGEX = "[\n\\s\"`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？ ]";
	public static final String FILE_NAME_SPECIAL_CHAR_REGEX = "[\\/:*?\"<>|\n]";
	/**
	 * 特殊字符
	 */
	public static final String SPECIAL_CHAR = "\"`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？ ";
	
	public static String nullToEmpty(@Nullable String str) {
		if (str == null) {
			return "";
		}
		return str;
	}
	
	public static String nullAsDefault(@Nullable String str, String def) {
		if (str == null) {
			return def;
		}
		return str;
	}
}
