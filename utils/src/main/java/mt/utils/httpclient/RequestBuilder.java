package mt.utils.httpclient;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.entity.mime.content.ContentBody;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2020/12/4
 */
public class RequestBuilder {
	private String url;
	private Request.HttpMethod method;
	private Request.ContentType contentType;
	private MultiValueMap<String, Object> headers;
	@Setter
	@Getter
	private Object body;
	private Map<String, Object> bodyMap;
	
	public static RequestBuilder create() {
		return new RequestBuilder();
	}
	
	public RequestBuilder setUrl(String url) {
		this.url = url;
		return this;
	}
	
	public RequestBuilder setMethod(Request.HttpMethod method) {
		this.method = method;
		return this;
	}
	
	public RequestBuilder setContentType(Request.ContentType contentType) {
		this.contentType = contentType;
		return this;
	}
	
	public RequestBuilder addHeader(String name, String value) {
		if (this.headers == null) {
			this.headers = new LinkedMultiValueMap<>();
		}
		this.headers.add(name, value);
		return this;
	}
	
	public RequestBuilder addHeader(String name, long value) {
		if (this.headers == null) {
			this.headers = new LinkedMultiValueMap<>();
		}
		this.headers.add(name, value);
		return this;
	}
	
	public RequestBuilder addBody(String name, String value) {
		return this.addObjectBody(name, value);
	}
	
	public RequestBuilder addBody(String name, long value) {
		return this.addObjectBody(name, value);
	}
	
	public RequestBuilder addBody(String name, int value) {
		return this.addObjectBody(name, value);
	}
	
	public RequestBuilder addBody(String name, double value) {
		return this.addObjectBody(name, value);
	}
	
	public RequestBuilder addBody(String name, ContentBody value) {
		return this.addObjectBody(name, value);
	}
	
	private RequestBuilder addObjectBody(String name, Object value) {
		if (this.bodyMap == null) {
			this.bodyMap = new HashMap<>();
		}
		this.bodyMap.put(name, value);
		return this;
	}
	
	public Request build() {
		Assert.notNull(url, "url can not be null");
		Assert.notNull(method, "method can not be null");
		Request request = new Request();
		request.setUrl(url);
		request.setMethod(method);
		request.setBody(body != null ? body : bodyMap);
		request.setContentType(contentType);
		request.setHeaders(headers);
		return request;
	}
}
