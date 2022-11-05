package mt.spring.tools.base.file;

/**
 * @Author Martin
 * @Date 2020/12/4
 */
public interface RecordFile {
	void finish(int chunkIndex);
	
	boolean hasDownload(int chunkIndex);
	
	void store();
	
	void clear();
}
