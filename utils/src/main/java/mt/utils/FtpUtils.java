package mt.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.utils.common.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;

/**
 * @Author Martin
 * @Date 2018/6/11
 */
@Slf4j
@Data
public class FtpUtils {
	
	private String host;
	private int port;
	private String username;
	private String password;
	
	private FtpUtils() {
	}
	
	public FtpUtils(String host, int port, String username, String password) {
		this.host = host;
		this.port = port;
		this.password = password;
		this.username = username;
	}
	
	private String dealPathname(String pathname) {
		Assert.notNull(pathname, "pathname can not be null");
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		return pathname;
	}
	
	public void download(String pathname, OutputStream outputStream) {
		pathname = dealPathname(pathname);
		FTPClient ftpClient = new FTPClient();
		try {
			ftpClient.connect(host, port);
			ftpClient.login(username, password);
			ftpClient.setFileTransferMode(10);
			ftpClient.setFileType(2);
			ftpClient.enterLocalPassiveMode();
			Assert.state(FTPReply.isPositiveCompletion(ftpClient.getReplyCode()), "isPositiveCompletion false");
			String directory = StringUtils.substringBeforeLast(pathname, "/");
			String filename = StringUtils.substringAfterLast(pathname, "/");
			Assert.state(ftpClient.changeWorkingDirectory(directory), "changeWorkingDirectory failed");
			InputStream inputStream = ftpClient.retrieveFileStream(filename);
			Assert.notNull(inputStream, "inputStream must not null");
			IOUtils.copy(inputStream, outputStream);
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(outputStream);
			ftpClient.logout();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			try {
				if (ftpClient.isConnected()) {
					ftpClient.disconnect();
				}
			} catch (IOException ignored) {
			}
			
		}
	}
	
	public interface GetInputStream {
		void handle(InputStream inputStream) throws IOException;
	}
	
	public void getInputStream(String pathname, GetInputStream get) {
		pathname = dealPathname(pathname);
		FTPClient ftpClient = new FTPClient();
		try {
			ftpClient.connect(host, port);
			ftpClient.login(username, password);
			ftpClient.setFileTransferMode(10);
			ftpClient.setFileType(2);
			ftpClient.enterLocalPassiveMode();
			Assert.state(FTPReply.isPositiveCompletion(ftpClient.getReplyCode()), "isPositiveCompletion false");
			String directory = StringUtils.substringBeforeLast(pathname, "/");
			String filename = StringUtils.substringAfterLast(pathname, "/");
			Assert.state(ftpClient.changeWorkingDirectory(directory), "changeWorkingDirectory failed");
			InputStream inputStream = ftpClient.retrieveFileStream(filename);
			Assert.notNull(inputStream, "inputStream must not null");
			get.handle(inputStream);
			IOUtils.closeQuietly(inputStream);
			ftpClient.logout();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			try {
				if (ftpClient.isConnected()) {
					ftpClient.disconnect();
				}
			} catch (IOException ignored) {
			}
			
		}
	}
	
	public String upload(String path, File file, String requestUrl) throws FileNotFoundException {
		return upload(path, new FileInputStream(file), requestUrl);
	}
	
	public FTPClient getInstance() throws IOException {
		FTPClient ftpClient = new FTPClient();
		ftpClient.connect(host, port);
		ftpClient.login(username, password);
		ftpClient.setFileTransferMode(10);
		ftpClient.setFileType(2);
		ftpClient.enterLocalPassiveMode();
		return ftpClient;
	}
	
	/**
	 * 上传文件
	 *
	 * @param path       上传文件路径+名称
	 * @param is         输入流
	 * @param requestUrl 请求地址
	 * @return 请求地址
	 */
	public String upload(String path, InputStream is, String requestUrl) {
		path = dealPathname(path);
		FTPClient ftpClient = new FTPClient();
		BufferedInputStream inputStream = null;
		
		try {
			inputStream = new BufferedInputStream(is);
			ftpClient.connect(host, port);
			ftpClient.login(username, password);
			ftpClient.setFileTransferMode(10);
			ftpClient.setFileType(2);
			ftpClient.enterLocalPassiveMode();
			Assert.state(FTPReply.isPositiveCompletion(ftpClient.getReplyCode()), "isPositiveCompletion is false");
			String directory = StringUtils.substringBeforeLast(path, "/");
			String filename = StringUtils.substringAfterLast(path, "/");
			if (!ftpClient.changeWorkingDirectory(directory)) {
				String[] paths = StringUtils.split(directory, "/");
				String p = "/";
				ftpClient.changeWorkingDirectory(p);
				for (String s : paths) {
					p += s + "/";
					if (!ftpClient.changeWorkingDirectory(p)) {
						ftpClient.makeDirectory(s);
						ftpClient.changeWorkingDirectory(p);
					}
				}
			}
			Assert.state(ftpClient.storeFile(filename, inputStream), "上传文件失败");
			ftpClient.logout();
			return requestUrl + "/" + path;
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(inputStream);
			try {
				if (ftpClient.isConnected()) {
					ftpClient.disconnect();
				}
			} catch (IOException ignored) {
			}
			
		}
	}
	
	private void disconnect(FTPClient ftpClient) {
		try {
			if (ftpClient.isConnected()) {
				ftpClient.disconnect();
			}
		} catch (IOException ignored) {
		}
	}
	
	/**
	 * 删除文件
	 *
	 * @param pathname
	 * @return
	 * @throws IOException
	 */
	public boolean delete(String pathname) throws IOException {
		FTPClient ftpClient = getInstance();
		boolean b = ftpClient.deleteFile(pathname);
		disconnect(ftpClient);
		return b;
	}
}
