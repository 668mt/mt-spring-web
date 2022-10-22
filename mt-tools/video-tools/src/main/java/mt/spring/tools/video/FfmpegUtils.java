package mt.spring.tools.video;

import lombok.extern.slf4j.Slf4j;
import mt.utils.common.Assert;
import org.jetbrains.annotations.NotNull;
import ws.schild.jave.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @Author Martin
 * @Date 2020/12/10
 */
@Slf4j
public class FfmpegUtils {
	private static FFMPEGLocator locator = new DefaultFFMPEGLocator();
	
	private static final Pattern SUCCESS_PATTERN = Pattern.compile("^\\s*video\\:\\S+\\s+audio\\:\\S+\\s+subtitle\\:\\S+\\s+global headers\\:\\S+.*$", Pattern.CASE_INSENSITIVE);
	
	public interface FfmpegWorker {
		void addArguments(FFMPEGExecutor ffmpeg);
	}
	
	/**
	 * 设置ffmpeg locator
	 *
	 * @param locator locator
	 */
	public static void setFfmpegLocator(FFMPEGLocator locator) {
		FfmpegUtils.locator = locator;
	}
	
	/**
	 * 执行ffmpeg命令
	 *
	 * @param ffmpegWorker 添加命令
	 */
	public static void doFfmpegJob(FfmpegWorker ffmpegWorker) {
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpegWorker.addArguments(ffmpeg);
		try {
			ffmpeg.execute();
			try (RBufferedReader reader = new RBufferedReader(new InputStreamReader(ffmpeg.getErrorStream()))) {
				String line;
				ConversionOutputAnalyzer outputAnalyzer = new ConversionOutputAnalyzer(0, null);
				while ((line = reader.readLine()) != null) {
					outputAnalyzer.analyzeNewLine(line);
				}
				if (outputAnalyzer.getLastWarning() != null) {
					String lastWarning = outputAnalyzer.getLastWarning();
					if (!SUCCESS_PATTERN.matcher(lastWarning).matches()) {
						throw new RuntimeException("No match for: " + SUCCESS_PATTERN + " in " + lastWarning);
					}
				}
			}
			int exitCode = ffmpeg.getProcessExitCode();
			if (exitCode != 0) {
				log.error("Process exit code: {}", exitCode);
				throw new RuntimeException("Exit code of ffmpeg encoding run is " + exitCode);
			}
		} catch (IOException | EncoderException e) {
			throw new RuntimeException(e);
		} finally {
			ffmpeg.destroy();
		}
	}
	
	/**
	 * 获取视频信息
	 *
	 * @param object   媒体文件
	 * @param timeout  超时
	 * @param timeUnit 超时单位
	 * @return 视频信息
	 * @throws Exception 异常
	 */
	public static mt.spring.tools.video.entity.VideoInfo getVideoInfo(MultimediaObject object, long timeout, TimeUnit timeUnit) throws Exception {
		ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
		try {
			Future<mt.spring.tools.video.entity.VideoInfo> future = singleThreadExecutor.submit(() -> {
				MultimediaInfo info = object.getInfo();
				ws.schild.jave.VideoInfo video = info.getVideo();
				Assert.notNull(video, "video parsed error");
				mt.spring.tools.video.entity.VideoInfo videoInfo = new mt.spring.tools.video.entity.VideoInfo();
				long duration = info.getDuration();
				videoInfo.setDuring(duration);
				if (duration > 0) {
					videoInfo.setVideoLength(secondToTime(duration / 1000));
				}
				videoInfo.setFormat(info.getFormat());
				videoInfo.setWidth(video.getSize().getWidth());
				videoInfo.setHeight(video.getSize().getHeight());
				videoInfo.setBitRate(video.getBitRate());
				videoInfo.setFrameRate(video.getFrameRate());
				videoInfo.setDecoder(video.getDecoder());
				return videoInfo;
			});
			return future.get(timeout, timeUnit);
		} finally {
			singleThreadExecutor.shutdownNow();
		}
	}
	
	/**
	 * 获取视频信息
	 *
	 * @param source   源文件
	 * @param timeout  超时
	 * @param timeUnit 超时单位
	 * @return 视频信息
	 * @throws Exception 异常
	 */
	public static mt.spring.tools.video.entity.VideoInfo getVideoInfo(File source, long timeout, TimeUnit timeUnit) throws Exception {
		return getVideoInfo(new MultimediaObject(source), timeout, timeUnit);
	}
	
	/**
	 * 获取视频信息
	 *
	 * @param url      视频地址
	 * @param timeout  超时
	 * @param timeUnit 超时单位
	 * @return 视频信息
	 * @throws Exception 异常
	 */
	public static mt.spring.tools.video.entity.VideoInfo getVideoInfo(URL url, long timeout, TimeUnit timeUnit) throws Exception {
		return getVideoInfo(new MultimediaObject(url), timeout, timeUnit);
	}
	
	/**
	 * 获取视频长度
	 *
	 * @param url 视频地址
	 * @return 视频长度
	 * @throws MalformedURLException 异常
	 * @throws EncoderException      异常
	 */
	public static String getVideoLength(String url) throws MalformedURLException, EncoderException {
		URL url1 = new URL(url);
		MultimediaObject object = new MultimediaObject(url1);
		MultimediaInfo info = object.getInfo();
		long duration = info.getDuration();
		return secondToTime(duration / 1000);
	}
	
	/**
	 * 获取视频长度
	 *
	 * @param file 文件
	 * @return 视频长度
	 * @throws MalformedURLException 异常
	 * @throws EncoderException      异常
	 */
	public static String getVideoLength(File file) throws MalformedURLException, EncoderException {
		MultimediaObject object = new MultimediaObject(file);
		MultimediaInfo info = object.getInfo();
		long duration = info.getDuration();
		return secondToTime(duration / 1000);
	}
	
	/**
	 * 将时间转换成00:00格式
	 *
	 * @param second 秒
	 * @return 转换结果
	 */
	public static String secondToTime(long second) {
		DecimalFormat format = new DecimalFormat("00");
		long days = second / 86400;            //转换天数
		second = second % 86400;            //剩余秒数
		long hours = second / 3600;            //转换小时
		second = second % 3600;                //剩余秒数
		long minutes = second / 60;            //转换分钟
		second = second % 60;                //剩余秒数
		String dd = format.format(days);
		String HH = format.format(hours);
		String mm = format.format(minutes);
		String ss = format.format(second);
		StringBuilder result = new StringBuilder();
		if (days > 0) {
			result.append(":").append(dd);
		}
		if (hours > 0) {
			result.append(":").append(HH);
		}
		result.append(":").append(mm);
		result.append(":").append(ss);
		return result.substring(1, result.length());
	}
	
	/**
	 * 截图
	 *
	 * @param srcFile 源文件
	 * @param desFile 目标文件
	 * @param width   宽度
	 * @param seconds 第几秒
	 * @throws Exception 异常
	 */
	public static void screenShot(File srcFile, File desFile, int width, int seconds) throws Exception {
		screenShot(new MultimediaObject(srcFile), desFile, width, seconds, 60, TimeUnit.SECONDS);
	}
	
	/**
	 * 截图
	 *
	 * @param url     视频地址
	 * @param desFile 目标文件
	 * @param width   宽度
	 * @param seconds 第几秒
	 * @throws Exception 异常
	 */
	public static void screenShot(URL url, File desFile, int width, int seconds) throws Exception {
		screenShot(new MultimediaObject(url), desFile, width, seconds, 60, TimeUnit.SECONDS);
	}
	
	/**
	 * 截图
	 *
	 * @param object   媒体文件
	 * @param desFile  目标文件
	 * @param width    宽度
	 * @param seconds  第几秒
	 * @param timeout  超时
	 * @param timeUnit 超时单位
	 * @throws Exception 异常
	 */
	public static void screenShot(MultimediaObject object, File desFile, int width, final int seconds, long timeout, TimeUnit timeUnit) throws Exception {
		if (desFile.exists()) {
			return;
		}
		ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
		try {
			Future<?> submit = singleThreadExecutor.submit(() -> {
				try {
					File parentFile = desFile.getParentFile();
					if (!parentFile.exists()) {
						parentFile.mkdirs();
					}
					double maxSeconds = 0;
					int s = seconds;
					if (s > 0) {
						try {
							long duration = object.getInfo().getDuration();
							maxSeconds = Math.floor(duration * 1.0 / 1000) - 5;
							if (maxSeconds < 0) {
								maxSeconds = 0;
							}
						} catch (Exception ignored) {
							s = 0;
						}
					}
					ScreenExtractor screenExtractor = new ScreenExtractor();
					VideoSize size = object.getInfo().getVideo().getSize();
					int height = (int) Math.ceil(width * size.getHeight() * 1.0 / size.getWidth());
					screenExtractor.render(object, width, height, (int) Math.min(maxSeconds, s), desFile, 1);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					throw new RuntimeException(e);
				}
			});
			submit.get(timeout, timeUnit);
		} finally {
			singleThreadExecutor.shutdownNow();
		}
	}
	
	/**
	 * 压缩图片
	 *
	 * @param srcFile 源文件
	 * @param desFile 目标文件
	 * @param width   宽度
	 * @throws Exception 异常
	 */
	public static void compressImage(File srcFile, File desFile, int width) throws Exception {
		screenShot(srcFile, desFile, width, 0);
	}
	
	/**
	 * 剪切视频
	 * 命令：ffmpeg -i 1.mp4 -ss 00:00:00 -to 00:00:20 -y -f mp4 -vcodec copy -acodec copy -q:v 1 thumb.mp4
	 *
	 * @param srcFile 源文件
	 * @param desFile 目标文件
	 * @param from    从，例：00:00:00
	 * @param to      到，例：00:00:20
	 */
	public static void cutVideo(@NotNull File srcFile, @NotNull File desFile, @NotNull String from, @NotNull String to) {
		doFfmpegJob(ffmpeg -> {
			ffmpeg.addArgument("-i");
			ffmpeg.addArgument(srcFile.getAbsolutePath());
			ffmpeg.addArgument("-ss");
			ffmpeg.addArgument(from);
			ffmpeg.addArgument("-to");
			ffmpeg.addArgument(to);
			ffmpeg.addArgument("-y");
			ffmpeg.addArgument("-f");
			ffmpeg.addArgument("mp4");
			ffmpeg.addArgument("-vcodec");
			ffmpeg.addArgument("copy");
			ffmpeg.addArgument("-acodec");
			ffmpeg.addArgument("copy");
			ffmpeg.addArgument("-q:v");
			ffmpeg.addArgument("1");
			ffmpeg.addArgument(desFile.getAbsolutePath());
		});
	}
	
	/**
	 * 生成预览视频
	 * 命令：ffmpeg -i 1.mp4 -vf "select='lte(mod(t, 122),1)',scale=400:-2,setpts=N/FRAME_RATE/TB" -an -y preview.mp4
	 *
	 * @param srcFile  源文件
	 * @param dstFile  目标文件，例如：preview.mp4
	 * @param segments 分段，每段1秒
	 * @param width    宽度
	 * @return 是否生成
	 * @throws Exception 异常
	 */
	public static boolean generatePreviewVideo(File srcFile, File dstFile, int segments, int width) throws Exception {
		return generatePreviewVideo(srcFile, dstFile, segments, width, -2);
	}
	
	/**
	 * 生成预览视频
	 * 命令：ffmpeg -i 1.mp4 -vf "select='lte(mod(t, 122),1)',scale=400:-2,setpts=N/FRAME_RATE/TB" -an -y preview.mp4
	 *F
	 * @param srcFile  源文件
	 * @param dstFile  目标文件，例如：preview.mp4
	 * @param segments 分段，每段1秒
	 * @param width    宽度
	 * @param height   高度
	 * @return 是否生成
	 * @throws Exception 异常
	 */
	public static boolean generatePreviewVideo(File srcFile, File dstFile, int segments, int width, int height) throws Exception {
		mt.spring.tools.video.entity.VideoInfo videoInfo = getVideoInfo(srcFile, 1, TimeUnit.MINUTES);
		long during = videoInfo.getDuring();
		long second = during / 1000 / segments;
		if (second > segments) {
			doFfmpegJob(ffmpeg -> {
				ffmpeg.addArgument("-i");
				ffmpeg.addArgument(srcFile.getAbsolutePath());
				ffmpeg.addArgument("-vf");
				ffmpeg.addArgument("\"select='lte(mod(t, " + second + "),1)',scale=" + width + ":" + height + ",setpts=N/FRAME_RATE/TB\"");
				ffmpeg.addArgument("-an");
				ffmpeg.addArgument("-y");
				ffmpeg.addArgument(dstFile.getAbsolutePath());
			});
			return true;
		}
		log.info("视频长度小于分片长度，不能生成预览视频,segments={}", segments);
		return false;
	}
}
