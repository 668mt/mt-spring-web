package mt.spring.tools.video.ffmpeg;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ws.schild.jave.ConversionOutputAnalyzer;
import ws.schild.jave.process.ProcessLocator;
import ws.schild.jave.process.ProcessWrapper;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;
import ws.schild.jave.utils.RBufferedReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * @Author Martin
 * @Date 2021/2/3
 */
@Slf4j
public class FfmpegJob {
	public final static ProcessLocator locator = new DefaultFFMPEGLocator();
	
	private static final Pattern SUCCESS_PATTERN = Pattern.compile("^\\s*video\\:\\S+\\s+audio\\:\\S+\\s+subtitle\\:\\S+\\s+global headers\\:\\S+.*$", Pattern.CASE_INSENSITIVE);
	
	public interface FfmpegWorker {
		void addArguments(ProcessWrapper ffmpeg);
	}
	
	public static void execute(FfmpegWorker ffmpegWorker) {
		execute(ffmpegWorker, -1, TimeUnit.MINUTES);
	}
	
	public static void execute(FfmpegWorker ffmpegWorker, long timeout, TimeUnit timeUnit) {
		String currentThreadName = Thread.currentThread().getName();
		ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
			Thread thread = new Thread(r);
			thread.setName(currentThreadName + "-ffmpeg-executor");
			return thread;
		});
		ProcessWrapper ffmpeg = locator.createExecutor();
		ffmpegWorker.addArguments(ffmpeg);
		ExecutorService readerExecutor = Executors.newFixedThreadPool(2, r -> {
			Thread thread = new Thread(r);
			thread.setName(currentThreadName + "-ffmpeg-log-reader");
			return thread;
		});
		try {
			Future<?> future = executorService.submit(() -> {
				try {
					ffmpeg.execute();
					InputStream errorStream = ffmpeg.getErrorStream();
					InputStream inputStream = ffmpeg.getInputStream();
					Future<?> read1 = readerExecutor.submit(() -> read(errorStream));
					Future<?> read2 = readerExecutor.submit(() -> read(inputStream));
					read1.get();
					read2.get();
					int exitCode = ffmpeg.getProcessExitCode();
					if (exitCode != 0) {
						log.error("Process exit code: {}", exitCode);
						throw new RuntimeException("Exit code of ffmpeg encoding run is " + exitCode);
					}
				} catch (Exception e) {
					log.error("ffmpeg执行失败:{}", e.getMessage(), e);
					throw new RuntimeException(e);
				}
			});
			if (timeout > 0 && timeUnit != null) {
				future.get(timeout, timeUnit);
			} else {
				future.get();
			}
		} catch (Exception e) {
			log.error("ffmpeg执行失败:{}", e.getMessage(), e);
			throw new RuntimeException(e);
		} finally {
			ffmpeg.destroy();
			try {
				executorService.shutdownNow();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			try {
				readerExecutor.shutdownNow();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}
	
	private static void read(InputStream inputStream) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				log.info(line);
			}
		} catch (Exception e) {
			log.error("read log error:{}", e.getMessage(), e);
		}
	}
}
