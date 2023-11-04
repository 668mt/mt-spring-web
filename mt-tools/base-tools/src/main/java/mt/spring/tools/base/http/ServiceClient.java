package mt.spring.tools.base.http;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mt.spring.tools.base.io.MyInputStreamBody;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2020/11/23
 */
@Slf4j
public class ServiceClient {
	private CloseableHttpClient httpClient;
	private HttpClientConnectionManager connectionManager;
	@Setter
	@Getter
	private ResponseErrorHandler responseErrorHandler;
	
	@Setter
	@Getter
	public int socketTimeout = 3600 * 1000;
	@Setter
	@Getter
	public int connectionTimeout = 50 * 1000;
	@Setter
	@Getter
	public int connectionRequestTimeout = -1;
	private Timer timer;
	@Setter
	@Getter
	private HttpHost proxy;
	@Getter
	@Setter
	private boolean disableRedirectHandling = false;
	
	public ServiceClient() {
		this.responseErrorHandler = new DefaultResponseErrorHandler();
	}
	
	public static class DisabledValidationTrustManager implements X509TrustManager {
		DisabledValidationTrustManager() {
		}
		
		@Override
		public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
		}
		
		@Override
		public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
		}
		
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}
	
	private HttpClientConnectionManager newConnectionManager(boolean disableSslValidation, int maxTotalConnections, int maxConnectionsPerRoute, long timeToLive, TimeUnit timeUnit, RegistryBuilder registryBuilder) {
		if (registryBuilder == null) {
			registryBuilder = RegistryBuilder.create().register("http", PlainConnectionSocketFactory.INSTANCE);
		}
		
		if (disableSslValidation) {
			try {
				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, new TrustManager[]{new DisabledValidationTrustManager()}, new SecureRandom());
				registryBuilder.register("https", new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE));
			} catch (NoSuchAlgorithmException | KeyManagementException var10) {
				log.warn("Error creating SSLContext", var10);
			}
		} else {
			registryBuilder.register("https", SSLConnectionSocketFactory.getSocketFactory());
		}
		
		Registry<ConnectionSocketFactory> registry = registryBuilder.build();
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry, null, null, null, timeToLive, timeUnit);
		connectionManager.setMaxTotal(maxTotalConnections);
		connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
		connectionManager.setValidateAfterInactivity(2 * 1000);
		return connectionManager;
	}
	
	private HttpClientConnectionManager newConnectionManager() {
		if (this.connectionManager == null) {
			synchronized (this) {
				if (this.connectionManager == null) {
					this.connectionManager = newConnectionManager(
						true,
						1024,
						1024,
						-1, TimeUnit.MILLISECONDS,
						null);
					timer = new Timer();
					timer.schedule(new TimerTask() {
						@Override
						public void run() {
							connectionManager.closeExpiredConnections();
						}
					}, 30000, 5000);
				}
			}
		}
		return connectionManager;
	}
	
	private CloseableHttpClient newHttpClient() {
		final RequestConfig requestConfig = RequestConfig.custom()
			.setConnectionRequestTimeout(connectionRequestTimeout)
			.setSocketTimeout(socketTimeout)
			.setConnectTimeout(connectionTimeout)
			.setContentCompressionEnabled(false)
			.setCookieSpec(CookieSpecs.STANDARD).build();
		HttpClientBuilder httpClientBuilder = HttpClients.custom()
			.setDefaultRequestConfig(requestConfig)
			.setConnectionManager(newConnectionManager());
		if (disableRedirectHandling) {
			httpClientBuilder.disableRedirectHandling();
		}
		if (proxy != null) {
			httpClientBuilder.setProxy(proxy);
		}
		return httpClientBuilder.build();
	}
	
	public CloseableHttpClient getHttpClient() {
		if (httpClient == null) {
			synchronized (this) {
				if (httpClient == null) {
					httpClient = newHttpClient();
				}
			}
		}
		return httpClient;
	}
	
	public void shutdown() {
		if (connectionManager != null) {
			connectionManager.shutdown();
		}
		if (httpClient != null) {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}
	
	
	/**
	 * 上传
	 *
	 * @param url
	 * @param inputStream
	 * @param params
	 * @param length
	 * @return
	 * @throws IOException
	 */
	public CloseableHttpResponse upload(String url, InputStream inputStream, Map<String, Object> params, long length) throws IOException {
		ContentType contentType = ContentType.create("multipart/form-data", StandardCharsets.UTF_8);
		RequestBuilder requestBuilder = RequestBuilder.create()
			.setUrl(url)
			.setContentType(Request.ContentType.APPLICATION_FORM_DATA)
			.addBody("file", new MyInputStreamBody(inputStream, contentType, "file", length));
		for (Map.Entry<String, Object> stringObjectEntry : params.entrySet()) {
			requestBuilder.addBody(stringObjectEntry.getKey(), stringObjectEntry.getValue().toString());
		}
		try {
			return execute(requestBuilder.build());
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}
	
	/**
	 * 异常处理
	 *
	 * @param response
	 * @return
	 */
	private CloseableHttpResponse handleResponse(CloseableHttpResponse response) {
		if (responseErrorHandler != null && responseErrorHandler.hasError(response)) {
			responseErrorHandler.handError(response);
		}
		return response;
	}
	
	/**
	 * get请求
	 *
	 * @param url
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public CloseableHttpResponse get(String url, Header... headers) throws IOException {
		BasicHttpRequest request = new BasicHttpRequest("GET", url);
		if (headers != null) {
			request.setHeaders(headers);
		}
		return handleResponse(getHttpClient().execute(getHttpHost(new URL(url)), request));
	}
	
	public String getAsString(String url, Header... headers) throws IOException {
		CloseableHttpResponse response = get(url, headers);
		return EntityUtils.toString(response.getEntity());
	}
	
	public InputStream getAsStream(String url, Header... headers) throws IOException {
		CloseableHttpResponse response = get(url, headers);
		return response.getEntity().getContent();
	}
	
	/**
	 * post请求
	 *
	 * @param url
	 * @param httpEntity
	 * @return
	 * @throws IOException
	 */
	public CloseableHttpResponse post(String url, HttpEntity httpEntity) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		httpPost.setEntity(httpEntity);
		return handleResponse(getHttpClient().execute(httpPost));
	}
	
	/**
	 * 删除
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public CloseableHttpResponse delete(String url) throws IOException {
		BasicHttpRequest request = new BasicHttpRequest("DELETE", url);
		return handleResponse(getHttpClient().execute(getHttpHost(new URL(url)), request));
	}
	
	private HttpHost getHttpHost(URL host) {
		return new HttpHost(host.getHost(), host.getPort(), host.getProtocol());
	}
	
	/**
	 * 执行
	 *
	 * @param request
	 * @return
	 * @throws IOException
	 */
	public CloseableHttpResponse execute(Request request) throws IOException {
		return handleResponse(getHttpClient().execute(HttpHost.create(request.getUrl()), request.buildRequest()));
	}
}
