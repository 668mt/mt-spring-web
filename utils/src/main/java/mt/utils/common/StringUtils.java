package mt.utils.common;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
public class StringUtils {
	/**
	 * 特殊字符正则表达式
	 */
	public static final String SPECIAL_CHAR_REGEX = "[\n\\s\"`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？ ]";
	/**
	 * 特殊字符
	 */
	public static final String SPECIAL_CHAR = "\"`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？ ";
	
	public static String nullToEmpty(String str) {
		if (str == null) {
			return "";
		}
		return str;
	}
}
