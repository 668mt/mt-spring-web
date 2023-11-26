package mt.utils.httpclient;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.message.BasicHttpRequest;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2020/11/23
 */
@Data
public class Request {
	private String url;
	private HttpMethod method;
	private ContentType contentType;
	private MultiValueMap<String, Object> headers;
	private Object body;
	
	public enum HttpMethod {
		GET, POST, DELETE, PUT
	}
	
	public enum ContentType {
		APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded"),
		APPLICATION_JSON("application/json"),
		APPLICATION_FORM_DATA("multipart/form-data");
		private final String value;
		private final String charset;
		
		ContentType(String value) {
			this(value, "utf-8");
		}
		
		ContentType(String value, String charset) {
			this.value = value;
			this.charset = charset;
		}
		
		public String getValue() {
			return value;
		}
		
		public String getCharset() {
			return charset;
		}
		
		public static ContentType valueOfByValue(String value) {
			Assert.notNull(value, "value can't be null");
			String[] split = value.split(";");
			for (ContentType contentType : ContentType.values()) {
				if (contentType.getValue().equalsIgnoreCase(split[0])) {
					return contentType;
				}
			}
			return null;
		}
	}
	
	@SneakyThrows
	public HttpRequest buildRequest() {
		if (contentType == null && headers != null) {
			for (Map.Entry<String, List<Object>> stringListEntry : headers.entrySet()) {
				if ("content-type".equalsIgnoreCase(stringListEntry.getKey())) {
					List<Object> value = stringListEntry.getValue();
					contentType = ContentType.valueOfByValue(value.get(0) + "");
					break;
				}
			}
		}
		HttpRequest httpRequest = null;
		switch (method) {
			case GET:
			case DELETE:
				httpRequest = new BasicHttpRequest(method.name(), url);
				break;
			case POST:
			case PUT:
				if (contentType == null) {
					contentType = ContentType.APPLICATION_JSON;
				}
				HttpEntityEnclosingRequestBase httpEntityEnclosingRequestBase = method == HttpMethod.POST ? new HttpPost(url) : new HttpPut(url);
				HttpEntity entity = null;
				switch (contentType) {
					case APPLICATION_JSON:
						entity = new StringEntity(body instanceof String ? (String) body : JSONObject.toJSONString(body), org.apache.http.entity.ContentType.APPLICATION_JSON);
						break;
					case APPLICATION_FORM_URLENCODED:
					case APPLICATION_FORM_DATA:
						MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
						org.apache.http.entity.ContentType parsedContentType = org.apache.http.entity.ContentType.parse(contentType.getValue());
						multipartEntityBuilder.setContentType(parsedContentType);
						if (body instanceof Map) {
							Map<String, Object> map = (Map<String, Object>) body;
							for (Map.Entry<String, Object> stringListEntry : map.entrySet()) {
								String name = stringListEntry.getKey();
								Object value = stringListEntry.getValue();
								if (value instanceof ContentBody) {
									ContentBody contentBody = (ContentBody) value;
									multipartEntityBuilder.addPart(name, contentBody);
								} else {
									multipartEntityBuilder.addTextBody(name, value.toString(), parsedContentType);
								}
							}
						}
						entity = multipartEntityBuilder.build();
						break;
				}
				httpEntityEnclosingRequestBase.setEntity(entity);
				httpRequest = httpEntityEnclosingRequestBase;
				break;
		}
		Assert.notNull(httpRequest, "httpRequest构建失败");
		if (headers != null) {
			for (Map.Entry<String, List<Object>> stringListEntry : headers.entrySet()) {
				for (Object o : stringListEntry.getValue()) {
					httpRequest.addHeader(stringListEntry.getKey(), o.toString());
				}
			}
		}
		return httpRequest;
	}
	
}
