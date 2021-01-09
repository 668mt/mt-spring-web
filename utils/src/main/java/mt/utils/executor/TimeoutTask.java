package mt.utils.executor;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2019/9/5
 */
@Slf4j
public class TimeoutTask implements Runnable {
	public TimeoutTask(Runnable runnable, Integer timeout, TimeUnit timeUnit) {
		this.runnable = runnable;
		this.timeout = timeout;
		this.timeoutUnit = timeUnit;
	}
	
	private final Runnable runnable;
	private final Integer timeout;
	private final TimeUnit timeoutUnit;
	
	@Override
	public void run() {
		String name = Thread.currentThread().getName();
		ExecutorService executorService = Executors.newSingleThreadExecutor((r) -> {
			Thread thread = new Thread(r);
			thread.setName(name);
			return thread;
		});
		Future<?> future = executorService.submit(runnable);
		try {
			if (timeout != null && timeout > 0) {
				future.get(timeout, timeoutUnit);
			} else {
				future.get();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} finally {
			executorService.shutdownNow();
		}
	}
	
}
