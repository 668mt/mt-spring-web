package mt.spring.tools.base.file;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Martin
 * @Date 2020/12/4
 */
public class PropertiesRecordFile implements RecordFile {
	private final Map<Object, Object> record;
	private final File recordFile;
	
	public PropertiesRecordFile(File recordFile) {
		this.recordFile = recordFile;
		File parentFile = recordFile.getParentFile();
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}
		record = new ConcurrentHashMap<>();
		if (recordFile.exists()) {
			try (FileInputStream inputStream = new FileInputStream(recordFile)) {
				Properties properties = new Properties();
				properties.load(inputStream);
				record.putAll(properties);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	@Override
	public synchronized void finish(int chunkIndex) {
		record.put("chunk" + chunkIndex, "true");
		store();
	}
	
	@Override
	public synchronized boolean hasDownload(int chunkIndex) {
		return "true".equalsIgnoreCase(record.get("chunk" + chunkIndex) + "");
	}
	
	@Override
	public synchronized void store() {
		try (FileOutputStream outputStream = new FileOutputStream(recordFile)) {
			Properties properties = new Properties();
			properties.putAll(record);
			properties.store(outputStream, null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public synchronized void clear() {
		if (recordFile.exists()) {
			FileUtils.deleteQuietly(recordFile);
		}
	}
}
