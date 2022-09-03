package mt.utils.spider;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public interface SpiderHandler<T> {
	/**
	 * item解析异常事件
	 *
	 * @param spider 当前对象
	 * @param url    地址
	 * @param item   项
	 * @param params 参数
	 */
	default void onItemParseException(Spider spider, String url, T item, JSONObject params) {
	}
	
	/**
	 * 解析异常事件
	 *
	 * @param spider 当前对象
	 * @param url    地址
	 * @param params 参数
	 */
	default void onParseException(Spider spider, String url, JSONObject params) {
	}
	
	/**
	 * 1. 解析条目
	 *
	 * @param spider   当前对象
	 * @param url      当前url
	 * @param document 页面document对象
	 * @return
	 */
	List<T> parseItems(Spider spider, String url, Document document, JSONObject params);
	
	/**
	 * 2. 为每个条目新增参数
	 *
	 * @param spider   当前对象
	 * @param url      当前url
	 * @param document 当前页面元素
	 * @param item     当前条目
	 * @param index    序号
	 * @param params   需要放参数的集合
	 */
	default void addItemParams(Spider spider, String url, Document document, T item, int index, JSONObject params) {
	}
	
	/**
	 * 3. 是否能够被抓取
	 *
	 * @param spider
	 * @param item   项
	 * @param params 参数
	 * @return
	 */
	default boolean canCatch(Spider spider, T item, JSONObject params) {
		return true;
	}
	
	
	/**
	 * 4. 解析下一页地址
	 *
	 * @param spider
	 * @param document 当前页面对象
	 * @return
	 */
	default String parseNextPageUrl(Spider spider, Document document) {
		return null;
	}
	
	/**
	 * 5. 解析下一级的url
	 *
	 * @param spider
	 * @param item
	 * @param params
	 * @return 如果返回不为null，那么抓取下一级
	 */
	default String parseNextModelUrl(Spider spider, T item, JSONObject params) {
		return null;
	}
	
	/**
	 * 6. 是否url有效
	 *
	 * @param url url地址
	 * @return 是否有效
	 */
	default boolean isUrlValid(String url) {
		return StringUtils.isNotBlank(url) && !url.contains("javascript");
	}
	
	/**
	 * 7. 每个条目解析完成的回调时间
	 *
	 * @param spider
	 * @param item   条目
	 * @param index  序号
	 * @param params 解析的参数
	 */
	default void onItemParsed(Spider spider, T item, int index, JSONObject params) {
	}
	
	/**
	 * 8. model抓取结束事件
	 *
	 * @param spider 当前对象
	 * @param params 参数
	 * @param items  抓取的所有条目
	 */
	default void onCatchFinished(Spider spider, JSONObject params, List<T> items) {
	}
	
	default String getForHtml(RestTemplate restTemplate, String url, String cookie, String userAgent, JSONObject params) {
		HttpHeaders httpHeaders = new HttpHeaders();
		if (StringUtils.isNotBlank(cookie)) {
			httpHeaders.set("Cookie", cookie);
		}
		if (userAgent != null) {
			httpHeaders.set("user-agent", userAgent);
		}
		
		if (StringUtils.isBlank(url)) {
			return null;
		}
		return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, httpHeaders), String.class).getBody();
	}
}