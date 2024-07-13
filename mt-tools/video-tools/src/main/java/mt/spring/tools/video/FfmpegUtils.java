package mt.spring.tools.video;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mt.spring.tools.video.ffmpeg.FfmpegJob;
import mt.spring.tools.video.ffmpeg.params.CutVideoParams;
import mt.utils.common.Assert;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.info.VideoInfo;
import ws.schild.jave.utils.Utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2020/12/10
 */
@Slf4j
public class FfmpegUtils {
	
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
				VideoInfo video = info.getVideo();
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
	 * 比特率计算
	 *
	 * @param size   文件大小，单位字节
	 * @param during 时长，单位毫秒
	 * @return 比特率
	 */
	public static long getBitRate(long size, long during) {
		if (during < 0 || size < 0) {
			return -1;
		}
		return size * 8 / during * 1000;
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
	 * @throws EncoderException 异常
	 */
	public static String getVideoLength(File file) throws EncoderException {
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
	public static void screenShot(File srcFile, File desFile, int width, int seconds, long timeout, TimeUnit timeUnit) throws Exception {
		mt.spring.tools.video.entity.VideoInfo videoInfo = getVideoInfo(srcFile, 2, TimeUnit.MINUTES);
		if (videoInfo.getWidth() < width) {
			width = videoInfo.getWidth();
		}
		imageScale(srcFile.getAbsolutePath(), desFile, width, -2, seconds, timeout, timeUnit);
	}
	
	/**
	 * 截图或放大
	 *
	 * @param pathOrUrl 视频地址或本地路径
	 * @param desFile   目标文件
	 * @param width     宽度，小于0表示不缩放
	 * @param seconds   第几秒
	 * @param timeout   超时，小于0表示不超时
	 * @param timeUnit  超时单位
	 * @throws Exception 异常
	 */
	public static void imageScale(String pathOrUrl, File desFile, int width, int height, int seconds, long timeout, TimeUnit timeUnit) throws Exception {
		//ffmpeg -i input.mp4 -ss 00:00:10 -f image2 -vframes 1 -vf "scale=400:-2" -qscale 1 -an -y test.jpg
		if (width <= 0) {
			width = -2;
		}
		if (height <= 0) {
			height = -2;
		}
		int finalHeight = height;
		int finalWidth = width;
		FfmpegJob.FfmpegWorker ffmpegWorker = ffmpeg -> {
			ffmpeg.addArgument("-i");
			ffmpeg.addArgument(pathOrUrl);
			ffmpeg.addArgument("-ss");
			ffmpeg.addArgument(Utils.buildTimeDuration(seconds * 1000L));
			ffmpeg.addArgument("-f");
			ffmpeg.addArgument("image2");
			ffmpeg.addArgument("-vframes");
			ffmpeg.addArgument("1");
			if (finalWidth > 0 || finalHeight > 0) {
				ffmpeg.addArgument("-vf");
				ffmpeg.addArgument("scale=" + finalWidth + ":" + finalHeight);
			}
			ffmpeg.addArgument("-q:v");
			ffmpeg.addArgument("1");
			ffmpeg.addArgument("-an");
			ffmpeg.addArgument("-y");
			ffmpeg.addArgument(desFile.getAbsolutePath());
		};
		FfmpegJob.execute(ffmpegWorker, timeout, timeUnit);
	}
	
	/**
	 * 连续截图20分钟
	 *
	 * @param srcFile 源文件
	 * @param dstPath 目标目录
	 */
	public static void screenshotsTwentyMinutes(@NotNull File srcFile, @NotNull File dstPath, long timeout, TimeUnit timeUnit) {
		screenshots(srcFile.getAbsolutePath(), dstPath, 0.01667, "00:00", "20:00", 400, timeout, timeUnit);
	}
	
	/**
	 * 连续截图
	 * ffmpeg -ss 00:00 -i 5.mp4 -f image2 -r 0.01667 -t 20:00 -filter:v scale=400:-1 thumb/%3d.jpg
	 *
	 * @param pathOrUrl  网络地址或本地路径
	 * @param dstPath    目标目录
	 * @param rate       每秒播放的帧  1 = 间隔秒数 * rate，例如5秒截图一次，那就是rate = 0.2
	 * @param startTime  开始时间，格式xx:xx，例如00:00
	 * @param duringRime 持续时间，格式xx:xx，例如20:00
	 * @param width      宽度
	 */
	public static void screenshots(@NotNull String pathOrUrl, @NotNull File dstPath, double rate, @NotNull String startTime, @NotNull String duringRime,
								   int width, long timeout, TimeUnit timeUnit) {
		dstPath.mkdirs();
		FfmpegJob.execute(ffmpeg -> {
			ffmpeg.addArgument("-ss");
			ffmpeg.addArgument(startTime);
			ffmpeg.addArgument("-i");
			ffmpeg.addArgument(pathOrUrl);
			ffmpeg.addArgument("-f");
			ffmpeg.addArgument("image2");
			ffmpeg.addArgument("-r");
			ffmpeg.addArgument(rate + "");
			ffmpeg.addArgument("-t");
			ffmpeg.addArgument(duringRime);
			ffmpeg.addArgument("-filter:v");
			ffmpeg.addArgument("scale=" + width + ":-1");
			ffmpeg.addArgument(dstPath.getAbsolutePath() + "/%3d.jpg");
		}, timeout, timeUnit);
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
		screenShot(srcFile, desFile, width, 0, -1, TimeUnit.SECONDS);
	}
	
	/**
	 * 剪切视频
	 * 命令：ffmpeg -i 1.mp4 -ss 00:00:00 -to 00:00:20 -y -f mp4 -vcodec copy -acodec copy -q:v 1 thumb.mp4
	 *
	 * @param pathOrUrl 网络地址或本地路径
	 * @param desFile   目标文件
	 * @param from      从，例：00:00:00
	 * @param to        到，例：00:00:20
	 */
	public static void cutVideo(@NotNull String pathOrUrl, @NotNull File desFile, @NotNull String from, @NotNull String to, @Nullable String vCodec, int timeout, TimeUnit timeUnit) throws Exception {
		if (StringUtils.isBlank(vCodec)) {
			vCodec = "copy";
		}
		String finalVCodec = vCodec;
		FfmpegJob.FfmpegWorker worker = ffmpeg -> {
			ffmpeg.addArgument("-i");
			ffmpeg.addArgument(pathOrUrl);
			ffmpeg.addArgument("-ss");
			ffmpeg.addArgument(from);
			ffmpeg.addArgument("-to");
			ffmpeg.addArgument(to);
			ffmpeg.addArgument("-y");
			ffmpeg.addArgument("-f");
			ffmpeg.addArgument("mp4");
			ffmpeg.addArgument("-vcodec");
			ffmpeg.addArgument(finalVCodec);
			ffmpeg.addArgument("-acodec");
			ffmpeg.addArgument("copy");
			ffmpeg.addArgument("-q:v");
			ffmpeg.addArgument("1");
			ffmpeg.addArgument(desFile.getAbsolutePath());
		};
		if (timeout > 0) {
			FfmpegJob.execute(worker, timeout, timeUnit);
		} else {
			FfmpegJob.execute(worker);
		}
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
	public static boolean generatePreviewVideo(@NotNull File srcFile, @NotNull File dstFile,
											   int segments, int width, @Nullable String vCodec,
											   long timeout, TimeUnit timeUnit
	) throws Exception {
		return generatePreviewVideo(srcFile, dstFile, segments, width, -2, vCodec, timeout, timeUnit);
	}
	
	/**
	 * 生成预览视频
	 * 命令：ffmpeg -i 1.mp4 -vf "select='lte(mod(t, 122),1)',scale=400:-2,setpts=N/FRAME_RATE/TB" -an -y preview.mp4
	 * F
	 *
	 * @param srcFile  源文件
	 * @param dstFile  目标文件，例如：preview.mp4
	 * @param segments 分段，每段1秒
	 * @param width    宽度
	 * @param height   高度
	 * @return 是否生成
	 * @throws Exception 异常
	 */
	public static boolean generatePreviewVideo(@NotNull File srcFile, @NotNull File dstFile, int segments, int width, int height, @Nullable String vCodec, long timeout, TimeUnit timeUnit) throws Exception {
		mt.spring.tools.video.entity.VideoInfo videoInfo = getVideoInfo(srcFile, 1, TimeUnit.MINUTES);
		long during = videoInfo.getDuring();
		long second = during / 1000 / segments;
		if (second > segments) {
			FfmpegJob.execute(ffmpeg -> {
				ffmpeg.addArgument("-i");
				ffmpeg.addArgument(srcFile.getAbsolutePath());
				if (StringUtils.isNotBlank(vCodec)) {
					ffmpeg.addArgument("-vcodec");
					ffmpeg.addArgument(vCodec);
				}
				ffmpeg.addArgument("-vf");
				ffmpeg.addArgument("\"select='lte(mod(t, " + second + "),1)',scale=" + width + ":" + height + ",setpts=N/FRAME_RATE/TB\"");
				ffmpeg.addArgument("-an");
				ffmpeg.addArgument("-y");
				ffmpeg.addArgument(dstFile.getAbsolutePath());
			}, timeout, timeUnit);
			return true;
		}
		log.info("视频长度小于分片长度，不能生成预览视频,segments={}", segments);
		return false;
	}
	
	/**
	 * 转换格式
	 * ffmpeg -i 1.wmv -y 1.mp4
	 *
	 * @param srcFile 源文件
	 * @param dstFile 目标文件
	 */
	public static void convert(@NotNull File srcFile, @NotNull File dstFile, @Nullable String vCodec) {
		convert(srcFile, dstFile, vCodec, -1, TimeUnit.MINUTES);
	}
	
	@SneakyThrows
	public static void convert(@NotNull File srcFile, @NotNull File dstFile, @Nullable String vCodec, int timeout, TimeUnit timeUnit) {
		FfmpegJob.FfmpegWorker worker = ffmpeg -> {
			ffmpeg.addArgument("-i");
			ffmpeg.addArgument(srcFile.getAbsolutePath());
			if (StringUtils.isNotBlank(vCodec)) {
				ffmpeg.addArgument("-vcodec");
				ffmpeg.addArgument(vCodec);
			}
			ffmpeg.addArgument("-vf");
			ffmpeg.addArgument("scale=iw:-2");
			ffmpeg.addArgument("-y");
			ffmpeg.addArgument(dstFile.getAbsolutePath());
		};
		FfmpegJob.execute(worker, timeout, timeUnit);
	}
	
	public static String secondsFormat(int seconds) {
		//格式化成00:00:00的格式
		return String.format("%02d:%02d:%02d", seconds / 3600, seconds % 3600 / 60, seconds % 60);
	}
	
	/**
	 * 剪切视频
	 *
	 * @param srcFile     源文件
	 * @param dstFile     目标文件
	 * @param fromSeconds 从第几秒开始
	 * @param toSeconds   到第几秒结束
	 * @param params      参数
	 * @throws Exception 异常
	 */
	public static void cutVideo(
		@NotNull File srcFile,
		@NotNull File dstFile,
		int fromSeconds,
		int toSeconds,
		@Nullable CutVideoParams params
	) throws Exception {
		if (params == null) {
			params = CutVideoParams.builder().build();
		}
		Integer width;
		Integer frameRate;
		if (params.getMaxWidth() != null || params.getMaxFrameRate() != null) {
			mt.spring.tools.video.entity.VideoInfo videoInfo = FfmpegUtils.getVideoInfo(srcFile, 2, TimeUnit.MINUTES);
			if (params.getMaxWidth() != null) {
				width = Math.min(videoInfo.getWidth(), params.getMaxWidth());
			} else {
				width = null;
			}
			if (params.getMaxFrameRate() != null) {
				frameRate = (int) Math.min(videoInfo.getFrameRate(), params.getMaxFrameRate());
			} else {
				frameRate = null;
			}
		} else {
			frameRate = null;
			width = null;
		}
		if (frameRate != null && frameRate < 1) {
			frameRate = null;
		}
		CutVideoParams finalParams = params;
		Integer finalFrameRate = frameRate;
		FfmpegJob.FfmpegWorker worker = ffmpeg -> {
			ffmpeg.addArgument("-i");
			ffmpeg.addArgument(srcFile.getAbsolutePath());
			ffmpeg.addArgument("-ss");
			FfmpegUtils.secondToTime(fromSeconds);
			ffmpeg.addArgument(secondsFormat(fromSeconds));
			ffmpeg.addArgument("-to");
			ffmpeg.addArgument(secondsFormat(toSeconds));
			ffmpeg.addArgument("-y");
			if (StringUtils.isNotBlank(finalParams.getFormat())) {
				ffmpeg.addArgument("-f");
				ffmpeg.addArgument(finalParams.getFormat());
			}
			ffmpeg.addArgument("-vcodec");
			if (StringUtils.isNotBlank(finalParams.getVCodec())) {
				ffmpeg.addArgument(finalParams.getVCodec());
			} else {
				ffmpeg.addArgument("copy");
			}
			ffmpeg.addArgument("-acodec");
			if (StringUtils.isNotBlank(finalParams.getACodec())) {
				ffmpeg.addArgument(finalParams.getACodec());
			} else {
				ffmpeg.addArgument("copy");
			}
			if (width != null) {
				ffmpeg.addArgument("-vf");
				ffmpeg.addArgument("scale=" + width + ":-2");
			}
			if (finalFrameRate != null) {
				ffmpeg.addArgument("-r");
				ffmpeg.addArgument(finalFrameRate + "");
			}
			ffmpeg.addArgument("-q:v");
			ffmpeg.addArgument("1");
			ffmpeg.addArgument(dstFile.getAbsolutePath());
		};
		long timeout = params.getTimeout() == null ? -1 : params.getTimeout();
		FfmpegJob.execute(worker, timeout, params.getTimeUnit());
	}
}
