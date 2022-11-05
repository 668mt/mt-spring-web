package mt.spring.tools.video.download;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.spring.tools.base.file.FastFileDownloader;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
	private final FastFileDownloader fastFileDownloader;
	
	public DefaultVideoDownloader(ServiceClient serviceClient) {
		this.serviceClient = serviceClient;
		this.fastFileDownloader = new FastFileDownloader(new HttpClientFastFileDownloaderHttpSupport(serviceClient));
	}
	
	@Override
	public void downloadMp4(@NotNull String mp4Url, @NotNull File desFile, boolean convertMp4, @Nullable DownloaderMessageListener downloaderMessageListener) throws IOException {
		desFile.getParentFile().mkdirs();
		File tempFile = new File(desFile.getParentFile(), desFile.getName() + ".tmp");
		try {
			updateMessage(downloaderMessageListener, "下载视频中...");
			fastFileDownloader.downloadLargeFile(mp4Url, tempFile);
			if (convertMp4) {
				updateMessage(downloaderMessageListener, "转换视频格式中...");
				convertToMp4(tempFile, desFile);
			} else {
				FileUtils.moveFile(tempFile, desFile);
			}
			updateMessage(downloaderMessageListener, desFile.getName() + "下载完成");
			FileUtils.deleteQuietly(tempFile);
		} catch (RuntimeException e) {
			if (cleanTempDirectoryWhenError) {
				FileUtils.deleteQuietly(tempFile);
				fastFileDownloader.deleteTempFiles(tempFile);
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
	
	private M3u8Info requestM3u8Info(@NotNull String url) throws IOException {
		String result = serviceClient.getAsString(url);
		Assert.notNull(result, "请求失败");
		List<String> lines = Arrays.stream(result.split("\n")).collect(Collectors.toList());
		if (features != null && features.contains(Feature.SUB_M3U8_LAST)) {
			Collections.reverse(lines);
		}
		Optional<String> m3u8Url = lines.stream().filter(s -> s.contains(".m3u8")).findFirst();
		String siteUrl = getSiteUrl(url);
		if (m3u8Url.isPresent()) {
			return requestM3u8Info(siteUrl + m3u8Url.get());
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
	public void downloadM3u8(@NotNull String m3u8Url, @NotNull File desFile, @Nullable DownloaderMessageListener downloaderMessageListener) throws IOException {
		log.info("下载{}到{}", m3u8Url, desFile);
		M3u8Info m3u8Info = requestM3u8Info(m3u8Url);
		Assert.notNull(m3u8Info, "解析失败");
		List<String> tsUrls = m3u8Info.getTsUrls();
		Assert.notEmpty(tsUrls, "获取tsUrls失败");
		File parentFile = desFile.getParentFile();
		File tempPath = new File(parentFile, DigestUtils.md5Hex(desFile.getName()) + "-temp");
		log.info("临时文件夹：{}", tempPath);
		tempPath.mkdirs();
		String keyUrl = m3u8Info.getKeyUrl();
		if (StringUtils.isNotBlank(keyUrl)) {
			updateMessage(downloaderMessageListener, "下载key:" + keyUrl);
			String key = RetryUtils.doWithRetry(() -> serviceClient.getAsString(keyUrl), retry, retryDelay, Throwable.class);
			if (StringUtils.isNotBlank(key)) {
				File keyFile = new File(tempPath, "key.key");
				try (FileOutputStream fileOutputStream = new FileOutputStream(keyFile)) {
					IOUtils.write(key.getBytes(StandardCharsets.UTF_8), fileOutputStream);
				}
			}
		}
		int total = tsUrls.size();
		AtomicInteger atomicInteger = new AtomicInteger(0);
		log.info("共{}个分片", total);
		ExecutorService executorService = Executors.newFixedThreadPool(threads);
		try {
			List<Future<?>> futures = new ArrayList<>();
			for (String s : tsUrls) {
				Future<?> future = executorService.submit(() -> {
					String tsName = getTsName(s);
					File desTsFile = new File(tempPath, tsName);
					if (desTsFile.exists()) {
						return;
					}
					RetryUtils.doWithRetry(() -> {
						fastFileDownloader.downloadNotUseThreadPool(s, desTsFile);
						return null;
					}, retry, retryDelay, Throwable.class);
					updateMessage(downloaderMessageListener, "下载ts文件中：" + (total + 1) + "/" + (atomicInteger.incrementAndGet()));
				});
				futures.add(future);
			}
			for (Future<?> future : futures) {
				future.get();
			}
			//下载完成
			updateMessage(downloaderMessageListener, "合并ts文件中...");
			merge(m3u8Info.getContent(), tempPath.getPath(), desFile);
			FileUtils.deleteDirectory(tempPath);
			updateMessage(downloaderMessageListener, "下载完成！");
		} catch (Exception e) {
			log.error(e.getMessage());
			if (cleanTempDirectoryWhenError) {
				FileUtils.deleteDirectory(tempPath);
			}
			throw new IOException("下载失败", e);
		} finally {
			executorService.shutdownNow();
		}
	}
	
	private void merge(String m3u8, String path, File desFile) throws IOException {
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
		
		FfmpegJob.execute(ffmpegExecutor -> {
			ffmpegExecutor.addArgument("-allowed_extensions");
			ffmpegExecutor.addArgument("ALL");
			ffmpegExecutor.addArgument("-i");
			ffmpegExecutor.addArgument(indexFile.getAbsolutePath());
			ffmpegExecutor.addArgument("-f");
			ffmpegExecutor.addArgument("mp4");
			ffmpegExecutor.addArgument("-c");
			ffmpegExecutor.addArgument("copy");
			ffmpegExecutor.addArgument("-y");
			ffmpegExecutor.addArgument(desFile.getAbsolutePath());
		});
	}
	
	
}
