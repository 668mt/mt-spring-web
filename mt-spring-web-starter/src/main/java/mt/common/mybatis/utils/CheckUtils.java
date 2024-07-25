package mt.common.mybatis.utils;

import mt.utils.common.Assert;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * @Author Martin
 * @Date 2024/7/25
 */
public class CheckUtils {
	private static final Pattern SQL_SYNTAX_PATTERN = Pattern.compile("(insert|delete|update|select|create|drop|truncate|grant|alter|deny|revoke|call|execute|exec|declare|show|rename|set)\\s+.*(into|from|set|where|table|database|view|index|on|cursor|procedure|trigger|for|password|union|and|or)|(select\\s*\\*\\s*from\\s+)", 2);
	private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("'.*(or|union|--|#|/*|;)", 2);
	
	/**
	 * 是否包含不安全字符
	 *
	 * @param value 字符串
	 * @return 是否包含不安全字符
	 */
	public static boolean isUnSafe(String value) {
		if (value == null) {
			return false;
		} else {
			return value.contains("(") || SQL_COMMENT_PATTERN.matcher(value).find() || SQL_SYNTAX_PATTERN.matcher(value).find();
		}
	}
	
	public static String mustOneField(@Nullable String key) {
		Assert.notBlank(key, "必须指定一个字段");
		Assert.state(!isUnSafe(key), "字段不安全:" + key);
		Assert.state(!key.contains(","), "只能指定一个字段:" + key);
		return key;
	}
}
