package mt.utils.executor;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mt.utils.common.TimeUtils;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


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
	
	public MtExecutor() {
		this(null, 5, 5, 0, TimeUnit.SECONDS, Integer.MAX_VALUE, false);
	}
	
	public MtExecutor(int threads) {
		this(null, threads, threads, 0, TimeUnit.SECONDS, Integer.MAX_VALUE, false);
	}
	
	public MtExecutor(String name, int threads) {
		this(name, threads, threads, 0, TimeUnit.SECONDS, Integer.MAX_VALUE, false);
	}
	
	public MtExecutor(String name,
					  int corePoolSize,
					  int maximumPoolSize,
					  long keepAliveTime,
					  TimeUnit unit,
					  int maxQueueSize, boolean allowCoreThreadTimeOut) {
		queue = new LinkedBlockingQueue<>(maxQueueSize);
		if (name != null) {
			CustomNameThreadFactory customNameThreadFactory = new CustomNameThreadFactory(name);
			threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<>(maxQueueSize), customNameThreadFactory);
		} else {
			threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<>(maxQueueSize));
		}
		threadPoolExecutor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
	}
	
	static class CustomNameThreadFactory implements ThreadFactory {
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;
		
		CustomNameThreadFactory(String name) {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() :
					Thread.currentThread().getThreadGroup();
			namePrefix = name + "-";
		}
		
		@Override
		public Thread newThread(@NotNull Runnable r) {
			Thread t = new Thread(group, r,
					namePrefix + threadNumber.getAndIncrement(),
					0);
			if (t.isDaemon()) {
				t.setDaemon(false);
			}
			if (t.getPriority() != Thread.NORM_PRIORITY) {
				t.setPriority(Thread.NORM_PRIORITY);
			}
			return t;
		}
	}
	
	@Getter
	@Setter
	private Event<T> event;
	@Getter
	@Setter
	private int taskTimeout = 0;
	@Setter
	@Getter
	private long delayMills;
	@Getter
	@Setter
	private TimeUnit taskTimeoutUnit = TimeUnit.MILLISECONDS;
	private final ThreadPoolExecutor threadPoolExecutor;
	@Getter
	@Setter
	private long startTime;
	@Getter
	@Setter
	private boolean showTimeInfo = true;
	/**
	 * 队列中的任务
	 */
	private final BlockingQueue<T> queue;
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
		return queue.contains(task) || runningJobs.contains(task);
	}
	
	/**
	 * 新增任务
	 *
	 * @param task
	 */
	public void submit(T task) {
		if (startTime <= 0) {
			startTime = System.currentTimeMillis();
		}
		queue.add(task);
		Task task1 = new Task(task);
		if (taskTimeout > 0) {
			threadPoolExecutor.submit(new TimeoutTask(task1, taskTimeout, taskTimeoutUnit));
		} else {
			threadPoolExecutor.submit(task1);
		}
	}
	
	public synchronized void submitIfNotExists(T task) {
		if (contains(task)) {
			return;
		}
		submit(task);
	}
	
	public int getQueueSize() {
		return queue.size();
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
	
	private final AtomicLong index = new AtomicLong(0);
	
	class Task implements Runnable {
		private final T task;
		
		public Task(T task) {
			this.task = task;
		}
		
		@Override
		public void run() {
			try {
				synchronized (MtExecutor.this) {
					runningJobs.add(task);
					queue.remove(task);
					if (showTimeInfo && index.get() > 0) {
						long endTime = System.currentTimeMillis();
						long cost = endTime - startTime;
						//平均耗时
						BigDecimal aver = BigDecimal.valueOf(cost).divide(BigDecimal.valueOf(index.get()), 3, RoundingMode.HALF_UP);
						//1秒多少任务
						double per = 0;
						if (aver.compareTo(BigDecimal.ZERO) > 0) {
							per = BigDecimal.valueOf(1000).divide(aver, 2, RoundingMode.HALF_UP).doubleValue();
						}
						long need = aver.multiply(BigDecimal.valueOf(queue.size())).setScale(3, RoundingMode.HALF_UP).longValue();
						log.info("执行第 {} 个任务，队列中还有：{}，任务已耗时{},还需{}全部完成，平均每秒{}个任务", index.incrementAndGet(), queue.size(), TimeUtils.getReadableTime(cost, 3), TimeUtils.getReadableTime(need, 3), per);
					} else {
						log.info("执行第 {} 个任务，队列中还有：{}", index.incrementAndGet(), queue.size());
					}
				}
				doJob(task);
				if (delayMills > 0) {
					Thread.sleep(delayMills);
				}
			} catch (Exception e) {
				if (event != null) {
					event.onError(MtExecutor.this, e, task);
				} else {
					log.error(e.getMessage(), e);
				}
			} finally {
				runningJobs.remove(task);
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
