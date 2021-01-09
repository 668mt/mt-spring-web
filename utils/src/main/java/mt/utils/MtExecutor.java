package mt.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * 固定线程池操作
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Company: </p>
 *
 * @author Martin
 * @date 2018-1-23 下午9:59:01
 */
@Slf4j
public abstract class MtExecutor<T> {
	
	public interface Event<T> {
		default void onError(MtExecutor<T> mtExecutor, Exception e, T task) {
			log.error(e.getMessage(), e);
		}
		
		default void onTaskFinished(MtExecutor<T> mtExecutor) {
			log.info("队列已完成！");
		}
	}
	
	public MtExecutor(int threads) {
		this(null, threads, threads, 0, TimeUnit.SECONDS, Integer.MAX_VALUE, false);
	}
	
	public MtExecutor(String name, int threads) {
		this(name, threads, threads, 0, TimeUnit.SECONDS, Integer.MAX_VALUE, false);
	}
	
	private MtExecutor() {
	}
	
	public MtExecutor(String name,
					  int corePoolSize,
					  int maximumPoolSize,
					  long keepAliveTime,
					  TimeUnit unit,
					  int maxQueueSize, boolean allowCoreThreadTimeOut) {
		queue = new LinkedBlockingQueue<>(maxQueueSize);
		if (name != null) {
			threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<>(maxQueueSize), r -> new Thread(r, name));
		} else {
			threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<>(maxQueueSize));
		}
		threadPoolExecutor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
	}
	
	@Getter
	@Setter
	private Event<T> event;
	@Getter
	@Setter
	private int taskTimeout = 0;
	@Getter
	@Setter
	private TimeUnit taskTimeoutUnit = TimeUnit.MILLISECONDS;
	private ThreadPoolExecutor threadPoolExecutor;
	/**
	 * 队列中的任务
	 */
	private BlockingQueue<T> queue;
	/**
	 * 执行中的任务
	 */
	@Getter
	private final List<T> runningJobs = Collections.synchronizedList(new ArrayList<>());
	
	/**
	 * 执行任务
	 *
	 * @param task
	 */
	public abstract void doJob(T task);
	
	public boolean contains(T task) {
		return queue.contains(task);
	}
	
	/**
	 * 新增任务
	 *
	 * @param task
	 */
	public void submit(T task) {
		queue.add(task);
		Task task1 = new Task(task);
		if (taskTimeout > 0) {
			threadPoolExecutor.submit(new TimeoutTask(task1, taskTimeout, taskTimeoutUnit));
		} else {
			threadPoolExecutor.submit(task1);
		}
	}
	
	/**
	 * 新增任务
	 *
	 * @param tasks
	 */
	public void submitAll(Collection<T> tasks) {
		for (T task : tasks) {
			submit(task);
		}
	}
	
	class Task implements Runnable {
		private final T task;
		
		public Task(T task) {
			this.task = task;
		}
		
		@Override
		public void run() {
			try {
				runningJobs.add(task);
				doJob(task);
			} catch (RuntimeException e) {
				if (event != null) {
					event.onError(MtExecutor.this, e, task);
				} else {
					throw e;
				}
			} finally {
				runningJobs.remove(task);
				queue.remove(task);
				if (queue.size() == 0 && event != null) {
					event.onTaskFinished(MtExecutor.this);
				}
			}
		}
	}
	
	public void shutdown() {
		threadPoolExecutor.shutdown();
	}
	
	public void shutdownNow() {
		threadPoolExecutor.shutdownNow();
	}
	
}
