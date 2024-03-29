package mt.spring.tools.video;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.utils.common.TimeUtils;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.AudioInfo;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.info.VideoInfo;
import ws.schild.jave.info.VideoSize;

import java.io.File;

/**
 * 视频压缩工具
 *
 * @Author Martin
 * @Date 2021/2/5
 */
@Data
@Slf4j
public class VideoCompressor {
	private int maxAudioSamplingRate = 44100;
	private int maxAudioBitRate = 128000;
	private int maxVideoFrameRate = 25;
	private int maxVideoBitRate = 2000000;
	private int maxVideoWidth = 1280;
	private String format = "mp4";
	private final File source;
	private final MultimediaObject sourceInfo;
	private MultimediaInfo info;
	private final File target;
	
	public VideoCompressor(File source, File target) {
		this.source = source;
		this.target = target;
		sourceInfo = new MultimediaObject(source);
		try {
			info = sourceInfo.getInfo();
		} catch (EncoderException e) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean needCompress() {
		VideoInfo video = info.getVideo();
		float frameRate = video.getFrameRate();
		int bitRate = video.getBitRate();
		int width = video.getSize().getWidth();
		AudioInfo audio = info.getAudio();
		int audioBitRate = audio.getBitRate();
		int samplingRate = audio.getSamplingRate();
		return frameRate > maxVideoFrameRate
				|| bitRate > maxVideoBitRate
				|| width > maxVideoWidth
				|| audioBitRate > maxAudioBitRate
				|| samplingRate > maxAudioSamplingRate;
	}
	
	private int getValue(int current, int max) {
		return Math.min(current, max);
	}
	
	private float getValue(float current, float max) {
		return Math.min(current, max);
	}
	
	/**
	 * 执行压缩
	 *
	 * @throws EncoderException 压缩异常
	 */
	public void compress() throws EncoderException {
		if (!needCompress()) {
			log.info("不需要压缩:{}", source);
			return;
		}
		long start = System.currentTimeMillis();
		log.info("开始压缩：{}", source);
		AudioInfo audioInfo = info.getAudio();
		VideoInfo videoInfo = info.getVideo();
		AudioAttributes audio = new AudioAttributes();
		audio.setCodec("aac");
		audio.setBitRate(getValue(audioInfo.getBitRate(), maxAudioBitRate));
		audio.setChannels(audioInfo.getChannels());
		audio.setSamplingRate(getValue(audioInfo.getSamplingRate(), maxAudioSamplingRate));
		
		VideoAttributes videoAttributes = new VideoAttributes();
		videoAttributes.setCodec("h264");
		videoAttributes.setBitRate(getValue(videoInfo.getBitRate(), maxVideoBitRate));
		videoAttributes.setFrameRate((int) getValue(videoInfo.getFrameRate(), maxVideoFrameRate));
		
		// 限制视频宽高
		int width = videoInfo.getSize().getWidth();
		if (width > maxVideoWidth) {
			int height = videoInfo.getSize().getHeight();
			float rat = (float) width / maxVideoWidth;
			videoAttributes.setSize(new VideoSize(maxVideoWidth, (int) (height / rat)));
		}
		
		EncodingAttributes attr = new EncodingAttributes();
		attr.setOutputFormat(format);
		attr.setAudioAttributes(audio);
		attr.setVideoAttributes(videoAttributes);
		attr.setEncodingThreads(Runtime.getRuntime().availableProcessors() / 2);
		Encoder encoder = new Encoder();
		encoder.encode(sourceInfo, target, attr);
		log.info("压缩总耗时：{}", TimeUtils.getReadableTime(System.currentTimeMillis() - start));
	}
}
