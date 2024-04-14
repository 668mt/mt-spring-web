package mt.spring.tools.video.download;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.spring.tools.base.file.FastFileDownloader;
import mt.spring.tools.base.file.FileDownloader;
import mt.spring.tools.base.file.HttpClientFastFileDownloaderHttpSupport;
import mt.spring.tools.base.http.ServiceClient;
import mt.spring.tools.video.ffmpeg.FfmpegJob;
import mt.utils.RegexUtils;
import mt.utils.common.Assert;
import mt.utils.common.RetryUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static mt.spring.tools.video.download.M3u8Utils.*;

/**
 * @Author Martin
 * @Date 2021/3/23
 */
@Slf4j
@Data
public class DefaultVideoDownloader implements VideoDownloader {
	private List<Feature> features;
	private int threads = 5;
	private boolean cleanTempDirectoryWhenError = false;
	private int retry = 0;
	private long retryDelay = 500;
	private final ServiceClient serviceClient;
	private volatile FileDownloader fileDownloader;
	
	public DefaultVideoDownloader(ServiceClient serviceClient) {
		this.serviceClient = serviceClient;
	}
	
	public DefaultVideoDownloader(ServiceClient serviceClient, FileDownloader fileDownloader) {
		this.serviceClient = serviceClient;
		this.fileDownloader = fileDownloader;
	}
	
	public FileDownloader getFileDownloader() {
		if (fileDownloader == null) {
			synchronized (this) {
				if (fileDownloader == null) {
					fileDownloader = new FastFileDownloader(threads, new HttpClientFastFileDownloaderHttpSupport(serviceClient));
				}
			}
		}
		return fileDownloader;
	}
	
	@Override
	public void shutdown() {
		if (serviceClient != null) {
			serviceClient.shutdown();
		}
		if (fileDownloader != null) {
			fileDownloader.shutdown();
		}
	}
	
	@Override
	public void downloadMp4(@NotNull String mp4Url, @NotNull File desFile, boolean convertMp4, @Nullable DownloaderMessageListener downloaderMessageListener) throws IOException {
		desFile.getParentFile().mkdirs();
		File tempFile = new File(desFile.getParentFile(), desFile.getName() + ".tmp");
		try {
			updateMessage(downloaderMessageListener, "下载视频中...");
			getFileDownloader().downloadFile(mp4Url, tempFile);
			if (convertMp4) {
				updateMessage(downloaderMessageListener, "转换视频格式中...");
				convertToMp4(tempFile, desFile);
			} else {
				FileUtils.moveFile(tempFile, desFile);
			}
			updateMessage(downloaderMessageListener, desFile.getName() + "下载完成");
			FileUtils.deleteQuietly(tempFile);
		} catch (Throwable e) {
			if (cleanTempDirectoryWhenError) {
				FileUtils.deleteQuietly(tempFile);
				getFileDownloader().deleteTempFiles(tempFile);
			}
			throw e;
		}
	}
	
	private void convertToMp4(File srcFile, File desFile) {
		FfmpegJob.execute(ffmpegExecutor -> {
			ffmpegExecutor.addArgument("-i");
			ffmpegExecutor.addArgument(srcFile.getAbsolutePath());
			ffmpegExecutor.addArgument("-c");
			ffmpegExecutor.addArgument("copy");
			ffmpegExecutor.addArgument("-y");
			ffmpegExecutor.addArgument(desFile.getAbsolutePath());
		});
	}
	
	@Override
	public M3u8Info getM3u8Info(@NotNull String url) throws IOException {
		String result = serviceClient.getAsString(url);
		Assert.notNull(result, "请求失败");
		List<String> lines = Arrays.stream(result.split("\n")).collect(Collectors.toList());
		if (features != null && features.contains(Feature.SUB_M3U8_LAST)) {
			Collections.reverse(lines);
		}
		Optional<String> m3u8Url = lines.stream().filter(s -> s.contains(".m3u8")).findFirst();
		String siteUrl = getSiteUrl(url);
		if (m3u8Url.isPresent()) {
			return getM3u8Info(siteUrl + m3u8Url.get());
		} else {
			List<String> tsUrls = Arrays.stream(result.split("\n"))
				.filter(s -> !s.startsWith("#"))
				.map(s -> getTsUrl(url, s))
				.collect(Collectors.toList());
			M3u8Info m3u8Info = new M3u8Info();
			m3u8Info.setTsUrls(tsUrls);
			m3u8Info.setContent(result);
			lines.stream().filter(line -> line.contains("X-KEY")).findFirst().ifPresent(s -> {
				String keyUrl = RegexUtils.findFirst(s, "URI=\"(.+?)\"", 1);
				if (!keyUrl.startsWith("http")) {
					keyUrl = keyUrl.startsWith("/") ? siteUrl + keyUrl : siteUrl + "/" + keyUrl;
				}
				m3u8Info.setKeyUrl(keyUrl);
			});
			m3u8Info.setBaseUrl(siteUrl);
			return m3u8Info;
		}
	}
	
	private void updateMessage(@Nullable DownloaderMessageListener downloaderMessageListener, @NotNull String message) {
		log.info(message);
		if (downloaderMessageListener != null) {
			downloaderMessageListener.update(message);
		}
	}
	
	@Override
	public M3u8Info downloadM3u8Files(@NotNull String m3u8Url, @NotNull File path, @Nullable DownloaderMessageListener downloaderMessageListener) throws IOException {
		log.info("下载{}到{}", m3u8Url, path);
		M3u8Info m3u8Info = getM3u8Info(m3u8Url);
		Assert.notNull(m3u8Info, "解析失败");
		List<String> tsUrls = m3u8Info.getTsUrls();
		Assert.notEmpty(tsUrls, "获取tsUrls失败");
		log.info("下载文件夹：{}", path);
		path.mkdirs();
		String keyUrl = m3u8Info.getKeyUrl();
		if (StringUtils.isNotBlank(keyUrl)) {
			updateMessage(downloaderMessageListener, "下载key:" + keyUrl);
			String key = RetryUtils.doWithRetry(() -> serviceClient.getAsString(keyUrl), retry, retryDelay, Throwable.class);
			if (StringUtils.isNotBlank(key)) {
				File keyFile = new File(path, "key.key");
				try (FileOutputStream fileOutputStream = new FileOutputStream(keyFile)) {
					IOUtils.write(key.getBytes(StandardCharsets.UTF_8), fileOutputStream);
				}
			}
		}
		int total = tsUrls.size();
		AtomicInteger atomicInteger = new AtomicInteger(0);
		log.info("共{}个分片", total);
		try {
			for (String s : tsUrls) {
				String tsName = getTsName(s);
				File desTsFile = new File(path, tsName);
				if (desTsFile.exists()) {
					continue;
				}
				RetryUtils.doWithRetry(() -> {
					updateMessage(downloaderMessageListener, "下载ts文件中：" + (total + 1) + "/" + (atomicInteger.incrementAndGet()));
					getFileDownloader().downloadFile(s, desTsFile);
					return null;
				}, retry, retryDelay, Throwable.class);
			}
			//生成index.m3u8
			updateMessage(downloaderMessageListener, "生成index.m3u8文件中...");
			File indexFile = generateM3u8IndexFile(m3u8Info.getContent(), path);
			m3u8Info.setIndexFile(indexFile);
			updateMessage(downloaderMessageListener, "ts文件下载完成！");
			return m3u8Info;
		} catch (Throwable e) {
			log.error("下载m3u8失败：{}", e.getMessage(), e);
			if (cleanTempDirectoryWhenError) {
				FileUtils.deleteDirectory(path);
			}
			throw new IOException("下载失败", e);
		}
	}
	
	@Override
	public void downloadM3u8(@NotNull String m3u8Url, @NotNull File desFile, @Nullable DownloaderMessageListener downloaderMessageListener) throws IOException {
		File parentFile = desFile.getParentFile();
		File tempPath = new File(parentFile, DigestUtils.md5Hex(desFile.getName()) + "-temp");
		log.info("临时文件夹：{}", tempPath);
		tempPath.mkdirs();
		
		try {
			M3u8Info m3u8Info = downloadM3u8Files(m3u8Url, tempPath, downloaderMessageListener);
			updateMessage(downloaderMessageListener, "合并ts文件中...");
			m3u8MergeToMp4(m3u8Info.getIndexFile(), desFile);
			//删除临时文件
			FileUtils.deleteDirectory(tempPath);
			updateMessage(downloaderMessageListener, "下载完成！");
		} catch (Throwable e) {
			log.error("下载m3u8失败：{}", e.getMessage(), e);
			if (cleanTempDirectoryWhenError) {
				FileUtils.deleteDirectory(tempPath);
			}
			throw new IOException("下载失败", e);
		}
	}
	
	private File generateM3u8IndexFile(String m3u8, File path) throws IOException {
		List<String> lines = new ArrayList<>();
		for (String s : m3u8.split("\n")) {
			if (s.contains("X-KEY")) {
				s = s.replaceAll("URI=\"(.+?)\"", "URI=\"key.key\"");
			} else if (!s.startsWith("#")) {
				s = getTsName(s);
			}
			lines.add(s);
		}
		File indexFile = new File(path, "index.m3u8");
		try (FileOutputStream fileOutputStream = new FileOutputStream(indexFile)) {
			IOUtils.write(StringUtils.join(lines, "\n").getBytes(StandardCharsets.UTF_8), fileOutputStream);
		}
		return indexFile;
	}
	
	protected void m3u8MergeToMp4(File indexFile, File desFile) {
		FfmpegJob.execute(ffmpegExecutor -> {
			ffmpegExecutor.addArgument("-allowed_extensions");
			ffmpegExecutor.addArgument("ALL");
			ffmpegExecutor.addArgument("-i");
			ffmpegExecutor.addArgument(indexFile.getAbsolutePath());
			ffmpegExecutor.addArgument("-f");
			ffmpegExecutor.addArgument("mp4");
			ffmpegExecutor.addArgument("-c");
			ffmpegExecutor.addArgument("copy");
//			//音频复制
//			ffmpegExecutor.addArgument("-c:a");
//			ffmpegExecutor.addArgument("copy");
//			//h264_nvenc
//			ffmpegExecutor.addArgument("-c:v");
//			ffmpegExecutor.addArgument("h264_nvenc");
			ffmpegExecutor.addArgument("-y");
			ffmpegExecutor.addArgument(desFile.getAbsolutePath());
		});
	}

//	public static void main(String[] args) throws Exception {
////		String url = "http://192.168.0.2:4100/mos/mukeyuan/202311/13394/index.m3u8?sign=Rx_hoKapctMV8Gw4H4BBHvSfe7PkW0OIksr2De5NLmdOdvRUUjIGRymZ5W-l8Q2zjekTXr-kLT730DBG_UbDUqdfvVhocxOsQZsOWvxHfH5L9d3JPkU6Lh7r-wM4gy7HILScvElu12WyKyHbjTayHy18mgCVch5rvFwX_yzcUtTYboQ4ovQ7NhGAEgV9z_gbUXTK9OYjXKm5fvp8_JiuPs1TXA==";
//		DefaultVideoDownloader defaultVideoDownloader = new DefaultVideoDownloader(new ServiceClient());
//		File file = new File("D:/test/bailu/test/testtarget.mp4");
//		File file2 = new File("D:/test/bailu/test/testtarget-output.mp4");
//		File path = new File("D:/test/bailu/test/m3u8");
////		defaultVideoDownloader.downloadM3u8(url, file, new DownloaderMessageListener() {
////			@Override
////			public void update(@NotNull String message) {
////				System.out.println(message);
////			}
////		});
////		M3u8Info m3u8Info = defaultVideoDownloader.downloadM3u8Files(url, path, new DownloaderMessageListener() {
////			@Override
////			public void update(@NotNull String message) {
////				System.out.println(message);
////			}
////		});
//		FfmpegUtils.cutVideo(new File(path, "index.m3u8").getAbsolutePath(), file2, "00:30:00", "00:31:00", "h264", 5, TimeUnit.MINUTES);
//	}
	
	public static void main(String[] args) throws ExecutionException, InterruptedException {
//		LoggingSystem.get(mt.utils.httpclient.ServiceClient.class.getClassLoader()).setLogLevel("root", LogLevel.INFO);
		String url = "http://192.168.0.2:4100/mos/mukeyuan/202311/13538/index.m3u8?sign=Ex_hoBBHKapctMV8Gw4H4vSfe7PkW0OIksr2De5NLmdOdvRV05Yac8YwS68M7nvdOQ7aXr-kLT730DBG_UbDUqdfver15BkayPQ8n8IrJwtWaFLJPkU6Lh7r-wM4gy7HILScvElu12WyKyHbjTayHy18mgCVch5rvFwX_yzcUtTYboQ4ovQ7NhGAEgV9z_gbUXTK9OYjXKm5fvp8_JiuPs1TXA==";
		ServiceClient serviceClient = new ServiceClient();
		DefaultVideoDownloader videoDownloader = new DefaultVideoDownloader(serviceClient);
		ExecutorService executorService = Executors.newFixedThreadPool(100);
		List<Future<?>> futures = new ArrayList<>();
		for (int i = 0; i < 200; i++) {
			int finalI = i;
			Future<?> submit = executorService.submit(() -> {
				try {
					File dstFile = new File("D:/test/bailu/test/down/" + finalI + ".mp4");
					videoDownloader.downloadM3u8(url, dstFile, null);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			futures.add(submit);
		}
		for (Future<?> future : futures) {
			future.get();
		}
		log.info("执行完毕");
		serviceClient.shutdown();
		executorService.shutdownNow();
	}
	
}
