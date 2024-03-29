package mt.spring.tools.video;

import lombok.extern.slf4j.Slf4j;
import mt.spring.tools.video.ffmpeg.FfmpegJob;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2021/2/3
 */
@Slf4j
public class HlsUtils {
	public static final int MB = 1024 * 1024;
	
	/**
	 * 将源文件转换成ts格式，并且分割成多个ts文件
	 *
	 * @param source            源文件
	 * @param target            目标文件 例：index.m3u8
	 * @param segmentMB         每段ts文件大小
	 * @param minSegmentSeconds 分段最小视频长度
	 */
	public static void convertToHlsBySize(File source, File target, int segmentMB, @Nullable Integer minSegmentSeconds, @Nullable String vCodec) {
		if (minSegmentSeconds == null) {
			minSegmentSeconds = 15;
		}
		//每个分片按10MB计算，但时长不能小于5s
		MultimediaObject object = new MultimediaObject(source);
		try {
			long duration = object.getInfo().getDuration();
			long length = source.length();
			double sizeMb = Math.ceil(length * 1.0 / MB);
			double perSecondMb = sizeMb / TimeUnit.MILLISECONDS.toSeconds(duration);
			int segmentSeconds = (int) Math.ceil(segmentMB * 1.0 / perSecondMb);
			if (segmentSeconds < minSegmentSeconds) {
				segmentSeconds = minSegmentSeconds;
			}
			convertToHlsBySeconds(source, target, segmentSeconds, vCodec);
		} catch (EncoderException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 将源文件转换成ts格式，并且分割成多个ts文件
	 *
	 * @param source         源文件
	 * @param target         目标文件 例：index.m3u8
	 * @param segmentSeconds 每段视频长度
	 * @param vCodec         视频编码
	 */
	public static void convertToHlsBySeconds(File source, File target, int segmentSeconds, @Nullable String vCodec) {
		File tsFile = new File(target.getParentFile(), target.getName() + ".ts");
		tsFile.getParentFile().mkdirs();
		try {
			convertTs(source, tsFile, vCodec);
			splitTs(tsFile, target, segmentSeconds);
		} finally {
			FileUtils.deleteQuietly(tsFile);
		}
	}
	
	/**
	 * 将源文件转换成.ts格式
	 * 命令：ffmpeg -y -i "IMG_8308.MOV"  -vcodec copy -acodec copy -vbsf h264_mp4toannexb test.ts
	 *
	 * @param source 源文件，例如：IMG_8308.MOV
	 * @param target 目标文件，例如：test.ts
	 * @param vCodec 视频编码
	 */
	public static void convertTs(File source, File target, @Nullable String vCodec) {
		log.info("转换为ts文件：{}", source);
		File parentFile = target.getParentFile();
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}
		try {
			FfmpegJob.execute(ffmpeg -> {
				ffmpeg.addArgument("-y");
				ffmpeg.addArgument("-i");
				ffmpeg.addArgument(source.getAbsolutePath());
				ffmpeg.addArgument("-vf");
				ffmpeg.addArgument("scale=iw:-2");
				ffmpeg.addArgument("-vcodec");
				if (StringUtils.isNotBlank(vCodec)) {
					ffmpeg.addArgument(vCodec);
				} else {
					ffmpeg.addArgument("copy");
				}
				ffmpeg.addArgument("-acodec");
				ffmpeg.addArgument("copy");
				ffmpeg.addArgument("-vbsf");
				ffmpeg.addArgument("h264_mp4toannexb");
				ffmpeg.addArgument(target.getAbsolutePath());
			});
		} catch (Exception e) {
			FfmpegJob.execute(ffmpeg -> {
				ffmpeg.addArgument("-y");
				ffmpeg.addArgument("-i");
				ffmpeg.addArgument(source.getAbsolutePath());
				ffmpeg.addArgument("-vf");
				ffmpeg.addArgument("scale=iw:-2");
				ffmpeg.addArgument("-vcodec");
				ffmpeg.addArgument("h264");
				ffmpeg.addArgument("-acodec");
				ffmpeg.addArgument("copy");
				ffmpeg.addArgument("-vbsf");
				ffmpeg.addArgument("h264_mp4toannexb");
				ffmpeg.addArgument(target.getAbsolutePath());
			});
		}
	}
	
	/**
	 * 将源文件分割成多个ts文件
	 * 命令：ffmpeg -i test.ts -c copy -map 0 -f segment -segment_list test.m3u8 -segment_time 60 "60s_%3d.ts"
	 *
	 * @param source         源文件
	 * @param target         目标文件 例：index.m3u8
	 * @param segmentSeconds 每段视频长度
	 */
	public static void splitTs(File source, File target, @Nullable Integer segmentSeconds) {
		log.info("分割ts文件：{}", source);
		target.getParentFile().mkdirs();
		if (segmentSeconds == null) {
			segmentSeconds = 30;
		}
		Integer finalSegmentSeconds = segmentSeconds;
		FfmpegJob.execute(ffmpeg -> {
			ffmpeg.addArgument("-i");
			ffmpeg.addArgument(source.getAbsolutePath());
			ffmpeg.addArgument("-c");
			ffmpeg.addArgument("copy");
			ffmpeg.addArgument("-map");
			ffmpeg.addArgument("0");
			ffmpeg.addArgument("-f");
			ffmpeg.addArgument("segment");
			ffmpeg.addArgument("-segment_list");
			ffmpeg.addArgument(target.getAbsolutePath());
			ffmpeg.addArgument("-segment_time");
			ffmpeg.addArgument(finalSegmentSeconds + "");
			ffmpeg.addArgument(new File(target.getParentFile(), "segment_%3d.ts").getAbsolutePath());
		});
	}
	
}
