package mt.spring.tools.video.ffmpeg.params;

import lombok.Builder;
import lombok.Data;

import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2023/11/15
 */
@Builder
@Data
public class CutVideoParams {
	private Long timeout;
	private TimeUnit timeUnit;
	private Integer maxWidth;
	private Integer maxFrameRate;
	private String vCodec;
	private String aCodec;
	private String format;
}
