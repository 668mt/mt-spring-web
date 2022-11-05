package mt.spring.tools.video.download;

import lombok.Data;

/**
 * @Author Martin
 */
@Data
public class ParseUrlResult {
	private String newUrl;
	private Object[] args;
}