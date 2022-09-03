package mt.utils.spider;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.utils.common.Assert;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * @Author Martin
 * @Date 2020/10/24
 */
@Data
@Slf4j
public class Spider {
	Spider() {
	}
	
	private String cookie;
	private String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:52.0) Gecko/20100101 Firefox/52.0";
	private HttpHeaders httpHeaders;
	private Integer readTimeout;
	private Integer connectTimeout;
	private RestTemplate restTemplate;
	private Model model;
	private final Map<String, ExecutorService> executorServiceMap = new ConcurrentHashMap<>();
	private volatile boolean stop = false;
	
	public static SpiderBuilder create() {
		return new SpiderBuilder();
	}
	
	public void stop() {
		this.stop = true;
		for (Map.Entry<String, ExecutorService> stringExecutorServiceEntry : executorServiceMap.entrySet()) {
			stringExecutorServiceEntry.getValue().shutdownNow();
		}
	}
	
	public void stopSafely() {
		this.stop = true;
		for (Map.Entry<String, ExecutorService> stringExecutorServiceEntry : executorServiceMap.entrySet()) {
			stringExecutorServiceEntry.getValue().shutdown();
		}
	}
	
	@Data
	public static class Model {
		private Long pageDelayMills;
		private AsyncConfig asyncConfig;
		private SpiderHandler<?> spiderHandler;
		private Integer maxPages;
		private Integer currentPage = 0;
		private Model next;
		
		public void addPage() {
			currentPage++;
		}
		
		public boolean isAsync() {
			return asyncConfig != null && asyncConfig.isAsync();
		}
		
	}
	
	@Data
	public static class AsyncConfig {
		private String name;
		private boolean async;
		private long delayMills;
		private int threadLine = 5;
		private Integer jobTimeoutSeconds;
	}
	
	private RestTemplate getRestTemplate() {
		if (restTemplate == null) {
			synchronized (this) {
				if (restTemplate == null) {
					SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
					if (readTimeout != null) {
						simpleClientHttpRequestFactory.setReadTimeout(Math.toIntExact(readTimeout));
					}
					if (connectTimeout != null) {
						simpleClientHttpRequestFactory.setConnectTimeout(Math.toIntExact(connectTimeout));
					}
					restTemplate = new RestTemplate();
					restTemplate.setRequestFactory(simpleClientHttpRequestFactory);
				}
			}
		}
		return restTemplate;
	}
	
	public void doAsyncSpider(String url) {
		doAsyncSpider(url, model, new JSONObject(), new ArrayList<>());
	}
	
	public void doAsyncSpider(String url, Model model) {
		doAsyncSpider(url, model, new JSONObject(), new ArrayList<>());
	}
	
	public void doAsyncSpider(String url, Model model, JSONObject params) {
		doAsyncSpider(url, model, params, new ArrayList<>());
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	public <T> List<T> doSpider(String url) {
		List list = new ArrayList();
		doAsyncSpider(url, null, null, list);
		return list;
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	public <T> List<T> doSpider(String url, JSONObject params) {
		List list = new ArrayList();
		doAsyncSpider(url, null, params, list);
		return list;
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	public <T> List<T> doSpider(String url, Model model, JSONObject params) {
		List list = new ArrayList();
		doAsyncSpider(url, model, params, list);
		return list;
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void doAsyncSpider(String url, Model model, JSONObject params, List outs) {
		if (stop) {
			log.debug("spider已经停止运行！");
			return;
		}
		if (model == null) {
			model = this.model;
		}
		Model findlModel = model;
		Assert.notNull(findlModel, "findlModel can't be null");
		if (params == null) {
			params = new JSONObject();
		}
		if (outs == null) {
			outs = new ArrayList();
		}
		findlModel.addPage();
		SpiderHandler spiderHandler = findlModel.getSpiderHandler();
		if (findlModel.getMaxPages() != null && findlModel.getCurrentPage() > findlModel.getMaxPages()) {
			log.info("已达到限定的最大页数：{},url:{}", findlModel.getMaxPages(), url);
			spiderHandler.onCatchFinished(this, params, outs);
			return;
		}
		
		if (!spiderHandler.isUrlValid(url)) {
			log.debug("url无效：{}", url);
			return;
		}
		String requestId = UUID.randomUUID().toString().substring(0, 8);
		log.debug("[{}]抓取{}", requestId, url);
		String html;
		List items;
		Document document;
		try {
			//获取html文本
			html = spiderHandler.getForHtml(getRestTemplate(), url, cookie, userAgent, params);
			Assert.state(StringUtils.isNotBlank(html), "抓取结果为空");
			document = Jsoup.parse(html);
			items = spiderHandler.parseItems(this, url, document, params);
		} catch (Exception e) {
			spiderHandler.onParseException(this, url, params);
			log.error("[" + requestId + "]抓取失败：" + e.getMessage(), e);
			throw e;
		}
		if (items != null) {
			log.debug("[{}]{} 抓取到{}项", requestId, url, items.size());
			for (int i = 0; i < items.size(); i++) {
				Object item = items.get(i);
				if (log.isTraceEnabled()) {
					log.trace("[{}]抓取到item{}", requestId, item.toString());
				}
				try {
					//参数，可用于传递给下一层的model
					JSONObject itemParams = new JSONObject();
					itemParams.putAll(params);
					spiderHandler.addItemParams(this, url, document, item, i, itemParams);
					if (!spiderHandler.canCatch(this, item, itemParams)) {
						//过滤掉不需要的内容
						continue;
					}
					if (findlModel.isAsync()) {
						//异步任务
						createAsyncWork(findlModel, spiderHandler, requestId, i, item, itemParams);
					} else {
						spiderHandler.onItemParsed(this, item, i, itemParams);
						Model nextModel = findlModel.getNext();
						if (nextModel != null) {
							doAsyncSpider(spiderHandler.parseNextModelUrl(Spider.this, item, itemParams), nextModel, itemParams, outs);
						} else {
							outs.add(item);
						}
					}
				} catch (Throwable e) {
					log.error("[" + requestId + "]解析失败" + e.getMessage(), e);
					spiderHandler.onItemParseException(this, url, item, params);
				}
			}
		}
		
		String nextPageUrl;
		try {
			nextPageUrl = spiderHandler.parseNextPageUrl(this, document);
		} catch (Exception e) {
			spiderHandler.onParseException(this, url, params);
			log.error("[" + requestId + "]抓取失败：" + e.getMessage(), e);
			throw e;
		}
		if (StringUtils.isNotBlank(nextPageUrl)) {
			log.debug("[{}]抓取下一页：{}", requestId, nextPageUrl);
			if (findlModel.getPageDelayMills() != null) {
				try {
					Thread.sleep(findlModel.getPageDelayMills());
				} catch (InterruptedException e) {
					log.error(e.getMessage(), e);
				}
			}
			doAsyncSpider(nextPageUrl, findlModel, params, outs);
		} else {
			if (CollectionUtils.isNotEmpty(outs)) {
				log.debug("[{}]抓取完成!", requestId);
				spiderHandler.onCatchFinished(this, params, outs);
			}
		}
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void createAsyncWork(Model findlModel, SpiderHandler spiderHandler, String requestId, int itemIndex, Object item, JSONObject itemParams) throws InterruptedException, ExecutionException, TimeoutException {
		final AsyncConfig asyncConfig = findlModel.getAsyncConfig();
		String key = StringUtils.isNotBlank(asyncConfig.getName()) ? asyncConfig.getName() : "default";
		ExecutorService executor = executorServiceMap.get(key);
		if (executor == null) {
			synchronized (this) {
				executor = executorServiceMap.get(key);
				if (executor == null) {
					log.debug("[{}]创建线程池", requestId);
					executor = Executors.newFixedThreadPool(asyncConfig.getThreadLine());
					executorServiceMap.put(key, executor);
				}
			}
		}
		
		Future<?> future = executor.submit(() -> {
			spiderHandler.onItemParsed(Spider.this, item, itemIndex, itemParams);
			if (findlModel.getNext() != null) {
				doAsyncSpider(spiderHandler.parseNextModelUrl(Spider.this, item, itemParams), findlModel.getNext(), itemParams, new ArrayList<>());
			}
		});
		if (asyncConfig.getDelayMills() > 0) {
			Thread.sleep(asyncConfig.getDelayMills());
		}
		if (asyncConfig.getJobTimeoutSeconds() != null) {
			future.get(asyncConfig.getJobTimeoutSeconds(), TimeUnit.SECONDS);
		}
	}
	
}
