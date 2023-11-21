package mt.spring.tools.video.download;

import lombok.Data;

import java.io.File;
import java.util.List;

/**
 * @Author Martin
 */
@Data
public class M3u8Info {
	private String baseUrl;
	private List<String> tsUrls;
	private String keyUrl;
	private String content;
	private File indexFile;
}