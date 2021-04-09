package mt.utils.common;

/**
 * @Author Martin
 * @Date 2021/4/2
 */
public class FileNameUtils {
	/**
	 * 获取文件名
	 *
	 * @param fileName 文件名
	 * @return 符合windows/linux文件命名规则的文件名
	 */
	public static String getSafetyFileName(String fileName) {
		Assert.notBlank(fileName, "文件名不能为空");
		return fileName.replaceAll(StringUtils.FILE_NAME_SPECIAL_CHAR_REGEX, "");
	}
}
