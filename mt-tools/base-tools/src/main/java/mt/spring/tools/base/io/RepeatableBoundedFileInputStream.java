package mt.spring.tools.base.io;


import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

@Slf4j
public class RepeatableBoundedFileInputStream extends InputStream {
	
	private final BoundedInputStream bis;
	private final FileChannel fileChannel;
	private long markPos;
	
	public RepeatableBoundedFileInputStream(BoundedInputStream bis) throws IOException {
		FileInputStream fin = (FileInputStream) bis.getWrappedInputStream();
		this.bis = bis;
		this.fileChannel = fin.getChannel();
		this.markPos = fileChannel.position();
	}
	
	@Override
	public void reset() throws IOException {
		bis.backoff(fileChannel.position() - markPos);
		fileChannel.position(markPos);
		log.trace("Reset to position " + markPos);
	}
	
	@Override
	public boolean markSupported() {
		return true;
	}
	
	@Override
	public void mark(int readlimit) {
		try {
			markPos = fileChannel.position();
		} catch (IOException e) {
			throw new RuntimeException("Failed to mark file position", e);
		}
		log.trace("File input stream marked at position " + markPos);
	}
	
	@Override
	public int available() throws IOException {
		return bis.available();
	}
	
	@Override
	public void close() throws IOException {
		bis.close();
		fileChannel.close();
	}
	
	@Override
	public int read() throws IOException {
		return bis.read();
	}
	
	@Override
	public long skip(long n) throws IOException {
		return bis.skip(n);
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return bis.read(b, off, len);
	}
	
	public InputStream getWrappedInputStream() {
		return this.bis;
	}
	
}

