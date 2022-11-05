package mt.spring.tools.base.file;

import lombok.Data;
import mt.spring.tools.base.http.ServiceClient;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2022/10/29
 */
@Data
public class HttpClientFastFileDownloaderHttpSupport implements FastFileDownloaderHttpSupport {
	private final ServiceClient serviceClient;
	
	public HttpClientFastFileDownloaderHttpSupport(ServiceClient serviceClient) {
		this.serviceClient = serviceClient;
	}
	
	@Override
	public long getFileLength(@NotNull String url) throws IOException {
		CloseableHttpResponse response = serviceClient.get(url);
		long length = 0;
		if (response.containsHeader("content-length")) {
			length = Long.parseLong(response.getFirstHeader("content-length").getValue());
		}
		return length;
	}
	
	@Override
	public InputStream getInputStream(@NotNull String url, @Nullable Map<String, String> headers) throws IOException {
		List<Header> httpHeaders = new ArrayList<>();
		if (headers != null) {
			for (Map.Entry<String, String> stringStringEntry : headers.entrySet()) {
				BasicHeader basicHeader = new BasicHeader(stringStringEntry.getKey(), stringStringEntry.getValue());
				httpHeaders.add(basicHeader);
			}
		}
		CloseableHttpResponse response = serviceClient.get(url, httpHeaders.toArray(new Header[0]));
		return response.getEntity().getContent();
	}
	
	public void shutdown() {
		if (serviceClient != null) {
			serviceClient.shutdown();
		}
	}
}
