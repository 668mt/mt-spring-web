package mt.spring.tools.video.ffmpeg;

import lombok.extern.slf4j.Slf4j;
import ws.schild.jave.ConversionOutputAnalyzer;
import ws.schild.jave.EncoderException;
import ws.schild.jave.process.ProcessLocator;
import ws.schild.jave.process.ProcessWrapper;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;

import java.io.IOException;
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
		ProcessWrapper ffmpeg = locator.createExecutor();
		ffmpegWorker.addArguments(ffmpeg);
		try {
			ffmpeg.execute();
			try (RBufferedReader reader = new RBufferedReader(new InputStreamReader(ffmpeg.getErrorStream()))) {
				String line;
				ConversionOutputAnalyzer outputAnalyzer = new ConversionOutputAnalyzer(0, null);
				while ((line = reader.readLine()) != null) {
					outputAnalyzer.analyzeNewLine(line);
				}
				if (outputAnalyzer.getLastWarning() != null) {
					String lastWarning = outputAnalyzer.getLastWarning();
					if (!SUCCESS_PATTERN.matcher(lastWarning).matches()) {
						throw new RuntimeException("No match for: " + SUCCESS_PATTERN + " in " + lastWarning);
					}
				}
			}
			int exitCode = ffmpeg.getProcessExitCode();
			if (exitCode != 0) {
				log.error("Process exit code: {}", exitCode);
				throw new RuntimeException("Exit code of ffmpeg encoding run is " + exitCode);
			}
		} catch (IOException | EncoderException e) {
			throw new RuntimeException(e);
		} finally {
			ffmpeg.destroy();
		}
	}
	
	public static void executeWithTimeout(FfmpegWorker ffmpegWorker, long time, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		try {
			Future<?> submit = executorService.submit(() -> execute(ffmpegWorker));
			submit.get(time, timeUnit);
		} finally {
			executorService.shutdownNow();
		}
	}
}
