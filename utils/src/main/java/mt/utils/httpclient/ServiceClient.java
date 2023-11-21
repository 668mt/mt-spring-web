package mt.utils.httpclient;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHttpRequest;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

/**
 * @Author Martin
 * @Date 2020/11/23
 */
@Slf4j
public class ServiceClient {
	private CloseableHttpClient httpClient;
	private HttpClientConnectionManager connectionManager;
	@Setter
	private ResponseErrorHandler responseErrorHandler;
	
	@Setter
	public int socketTimeout = 3600 * 1000;
	@Setter
	public int connectionTimeout = 3000;
	@Setter
	public int connectionRequestTimeout = 3000;
	private Timer timer;
	
	public ServiceClient() {
		this.httpClient = newHttpClient();
		this.responseErrorHandler = new DefaultResponseErrorHandler();
	}
	
	static class DisabledValidationTrustManager implements X509TrustManager {
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
	
	public HttpClientConnectionManager newConnectionManager(boolean disableSslValidation, int maxTotalConnections, int maxConnectionsPerRoute, long timeToLive, TimeUnit timeUnit, RegistryBuilder registryBuilder) {
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
	
	public HttpClientConnectionManager newConnectionManager() {
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
	
	public CloseableHttpClient newHttpClient() {
		final RequestConfig requestConfig = RequestConfig.custom()
			.setConnectionRequestTimeout(connectionRequestTimeout)
			.setSocketTimeout(socketTimeout)
			.setConnectTimeout(connectionTimeout)
			.setContentCompressionEnabled(false)
			.setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		return HttpClients.custom().setDefaultRequestConfig(requestConfig)
			.setConnectionManager(newConnectionManager()).disableRedirectHandling()
			.build();
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
	
	
	private CloseableHttpResponse handleResponse(CloseableHttpResponse response) {
		if (responseErrorHandler != null && responseErrorHandler.hasError(response)) {
			responseErrorHandler.handError(response);
		}
		return response;
	}
	
	public CloseableHttpResponse get(String url, Header... headers) throws IOException {
		BasicHttpRequest request = new BasicHttpRequest("GET", url);
		if (headers != null) {
			request.setHeaders(headers);
		}
		return handleResponse(getHttpClient().execute(getHttpHost(new URL(url)), request));
	}
	
	public CloseableHttpResponse post(String url, HttpEntity httpEntity) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		httpPost.setEntity(httpEntity);
		return handleResponse(getHttpClient().execute(httpPost));
	}
	
	public CloseableHttpResponse delete(String url) throws IOException {
		BasicHttpRequest request = new BasicHttpRequest("DELETE", url);
		return handleResponse(getHttpClient().execute(getHttpHost(new URL(url)), request));
	}
	
	public HttpHost getHttpHost(URL host) {
		return new HttpHost(host.getHost(), host.getPort(), host.getProtocol());
	}
	
	public CloseableHttpResponse execute(Request request) throws IOException {
		return handleResponse(getHttpClient().execute(getHttpHost(new URL(request.getUrl())), request.buildRequest()));
	}
	
}
