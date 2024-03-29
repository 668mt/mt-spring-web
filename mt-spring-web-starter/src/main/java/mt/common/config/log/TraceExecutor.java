package mt.common.config.log;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
public class TraceExecutor implements ExecutorService {
	private final ExecutorService proxy;
	
	public TraceExecutor(ExecutorService proxy) {
		this.proxy = proxy;
	}
	
	@Override
	public void execute(@NotNull Runnable command) {
		proxy.execute(new TraceRunnable(command));
	}
	
	@Override
	public void shutdown() {
		proxy.shutdown();
	}
	
	@NotNull
	@Override
	public List<Runnable> shutdownNow() {
		return proxy.shutdownNow();
	}
	
	@Override
	public boolean isShutdown() {
		return proxy.isShutdown();
	}
	
	@Override
	public boolean isTerminated() {
		return proxy.isTerminated();
	}
	
	@Override
	public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
		return proxy.awaitTermination(timeout, unit);
	}
	
	@NotNull
	@Override
	public <T> Future<T> submit(@NotNull Callable<T> task) {
		return proxy.submit(new TraceCallable<>(task));
	}
	
	@NotNull
	@Override
	public <T> Future<T> submit(@NotNull Runnable task, T result) {
		return proxy.submit(new TraceRunnable(task), result);
	}
	
	@NotNull
	@Override
	public Future<?> submit(@NotNull Runnable task) {
		return proxy.submit(new TraceRunnable(task));
	}
	
	@NotNull
	@Override
	public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
		List<TraceCallable<T>> list = tasks.stream().map(TraceCallable::new).collect(Collectors.toList());
		return proxy.invokeAll(list);
	}
	
	@NotNull
	@Override
	public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
		List<TraceCallable<T>> list = tasks.stream().map(TraceCallable::new).collect(Collectors.toList());
		return proxy.invokeAll(list, timeout, unit);
	}
	
	@NotNull
	@Override
	public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		List<TraceCallable<T>> list = tasks.stream().map(TraceCallable::new).collect(Collectors.toList());
		return proxy.invokeAny(list);
	}
	
	@Override
	public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		List<TraceCallable<T>> list = tasks.stream().map(TraceCallable::new).collect(Collectors.toList());
		return proxy.invokeAny(list, timeout, unit);
	}
}
