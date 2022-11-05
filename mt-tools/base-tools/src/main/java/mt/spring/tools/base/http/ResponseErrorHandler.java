package mt.spring.tools.base.http;

import org.apache.http.client.methods.CloseableHttpResponse;

public interface ResponseErrorHandler {
	boolean hasError(CloseableHttpResponse response);
	
	void handError(CloseableHttpResponse response);
}