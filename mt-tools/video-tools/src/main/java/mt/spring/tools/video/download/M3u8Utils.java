package mt.spring.tools.video.download;

import mt.utils.RegexUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2022/10/29
 */
public class M3u8Utils {
	/**
	 * 获取url域名
	 *
	 * @param url 视频地址
	 * @return
	 */
	public static String getSiteUrl(@NotNull String url) {
		return RegexUtils.findFirst(url, "(http(s)*://.+?)/.+", 1);
	}
	
	/**
	 * @param url           视频地址全路径
	 * @param tsRelativeUrl 相对路径
	 * @return 获取ts全路径
	 */
	public static String getTsUrl(@NotNull String url, @NotNull String tsRelativeUrl) {
		String tsUrl;
		if (tsRelativeUrl.startsWith("http")) {
			tsUrl = tsRelativeUrl;
		} else if (tsRelativeUrl.startsWith("/")) {
			String siteUrl = getSiteUrl(url);
			tsUrl = siteUrl + tsRelativeUrl;
		} else {
			int queryIndex = url.indexOf("?");
			String preUrl = url;
			if (queryIndex != -1) {
				preUrl = preUrl.substring(0, queryIndex);
			}
			int lastIndexOf = preUrl.lastIndexOf("/");
			String urlPrefix = preUrl.substring(0, lastIndexOf);
			tsUrl = urlPrefix + "/" + tsRelativeUrl;
		}
		return tsUrl.trim();
	}
	
	/**
	 * 获取ts文件名
	 *
	 * @param tsUrl ts路径
	 * @return ts文件名
	 */
	public static String getTsName(String tsUrl) {
		int i = tsUrl.lastIndexOf("?");
		if (i != -1) {
			tsUrl = tsUrl.substring(0, i);
		}
		int index = tsUrl.lastIndexOf("/");
		if (index != -1) {
			return tsUrl.substring(index + 1);
		}
		return tsUrl;
	}
	
	/**
	 * 解析视频地址
	 *
	 * @param url 视频地址
	 * @return
	 * @throws IOException
	 */
	public static ParseUrlResult parseUrl(@NotNull String url) throws IOException {
		int queryIndex = url.indexOf("?");
		List<String> args = new ArrayList<>();
		String newUrl = url;
		if (queryIndex != -1) {
			String queryString = url.substring(queryIndex + 1);
			StringBuilder newQueryString = new StringBuilder();
			String[] split = queryString.split("&");
			for (int i = 0; i < split.length; i++) {
				if (split[i].contains("=")) {
					String[] split1 = split[i].split("=");
					args.add(URLDecoder.decode(split1[1], "UTF-8"));
					newQueryString.append(split1[0]).append("={").append(i).append("}");
				} else {
					newQueryString.append(split[i]);
				}
				if (i != split.length - 1) {
					newQueryString.append("&");
				}
			}
			newUrl = url.substring(0, queryIndex) + "?" + newQueryString;
		}
		ParseUrlResult parseUrlResult = new ParseUrlResult();
		parseUrlResult.setNewUrl(newUrl);
		parseUrlResult.setArgs(args.toArray());
		return parseUrlResult;
	}
	
}
