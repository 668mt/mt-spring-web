package mt.utils.spider;

/**
 * @Author Martin
 * @Date 2020/10/25
 */
public class SpiderBuilder {
	private final Spider spider = new Spider();
	
	public SpiderBuilder cookie(String cookie) {
		spider.setCookie(cookie);
		return this;
	}
	
	public SpiderBuilder readTimeout(int readTimeout) {
		spider.setReadTimeout(readTimeout);
		return this;
	}
	
	public SpiderBuilder connectTimeout(int connectTimeout) {
		spider.setConnectTimeout(connectTimeout);
		return this;
	}
	
	public SpiderModelBuilder model() {
		SpiderModelBuilder spiderModelBuilder = new SpiderModelBuilder(this);
		Spider.Model model = spiderModelBuilder.buildModel();
		spider.setModel(model);
		return spiderModelBuilder;
	}
	
	public Spider build() {
		return spider;
	}
	
	
	public static class SpiderModelBuilder {
		private SpiderBuilder spiderBuilder;
		private final Spider.Model model = new Spider.Model();
		
		public SpiderModelBuilder() {
		}
		
		public SpiderModelBuilder(SpiderBuilder spiderBuilder) {
			this.spiderBuilder = spiderBuilder;
		}
		
		public AsyncConfigBuilder async() {
			return async("default");
		}
		
		public AsyncConfigBuilder async(String name) {
			AsyncConfigBuilder asyncConfigBuilder = new AsyncConfigBuilder(this);
			Spider.AsyncConfig asyncConfig = asyncConfigBuilder.buildAsyncConfig();
			asyncConfig.setAsync(true);
			asyncConfig.setName(name);
			model.setAsyncConfig(asyncConfig);
			return asyncConfigBuilder;
		}
		
		public SpiderModelBuilder handler(SpiderHandler<?> handler) {
			model.setSpiderHandler(handler);
			return this;
		}
		
		/**
		 * 抓取下一页的间隔时间
		 *
		 * @param pageDelayMills 间隔时间，单位毫秒
		 * @return
		 */
		public SpiderModelBuilder pageDelayMills(long pageDelayMills) {
			model.setPageDelayMills(pageDelayMills);
			return this;
		}
		
		/**
		 * 抓取的页数
		 *
		 * @param maxPages 抓取的页数
		 * @return
		 */
		public SpiderModelBuilder maxPages(int maxPages) {
			model.setMaxPages(maxPages);
			return this;
		}
		
		/**
		 * 进入下一页抓取模型，SpiderHandler需要复写parseNextModelUrl方法
		 *
		 * @return
		 */
		public SpiderModelBuilder next() {
			SpiderModelBuilder spiderModelBuilder = new SpiderModelBuilder(spiderBuilder);
			Spider.Model model = spiderModelBuilder.buildModel();
			this.model.setNext(model);
			return spiderModelBuilder;
		}
		
		public Spider build() {
			return this.spiderBuilder.build();
		}
		
		private Spider.Model buildModel() {
			return model;
		}
	}
	
	
	public static class AsyncConfigBuilder {
		private final Spider.AsyncConfig asyncConfig = new Spider.AsyncConfig();
		private final SpiderModelBuilder spiderModelBuilder;
		
		public AsyncConfigBuilder(SpiderModelBuilder spiderModelBuilder) {
			this.spiderModelBuilder = spiderModelBuilder;
		}
		
		/**
		 * 设置线程数
		 *
		 * @param threadLine 线程数
		 * @return
		 */
		public AsyncConfigBuilder threadLine(int threadLine) {
			asyncConfig.setThreadLine(threadLine);
			return this;
		}
		
		/**
		 * 设置延迟时间
		 *
		 * @param delayMills 延迟时间，单位毫秒
		 * @return
		 */
		public AsyncConfigBuilder delayMills(long delayMills) {
			asyncConfig.setDelayMills(delayMills);
			return this;
		}
		
		/**
		 * 设置任务超时秒数
		 *
		 * @param jobTimeoutSeconds 任务超时秒数
		 * @return
		 */
		public AsyncConfigBuilder jobTimeoutSeconds(int jobTimeoutSeconds) {
			asyncConfig.setJobTimeoutSeconds(jobTimeoutSeconds);
			return this;
		}
		
		public AsyncConfigBuilder name(String name) {
			asyncConfig.setName(name);
			return this;
		}
		
		public SpiderModelBuilder and() {
			return spiderModelBuilder;
		}
		
		private Spider.AsyncConfig buildAsyncConfig() {
			return asyncConfig;
		}
		
		public Spider build() {
			return this.spiderModelBuilder.build();
		}
	}
}
