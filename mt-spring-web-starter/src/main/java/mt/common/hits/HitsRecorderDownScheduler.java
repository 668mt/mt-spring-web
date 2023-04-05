package mt.common.hits;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2023/4/2
 */
@Data
@Slf4j
public class HitsRecorderDownScheduler {
	private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
	private final List<HitsRecorder<?, ?>> hitsRecorders;
	
	public HitsRecorderDownScheduler(List<HitsRecorder<?, ?>> hitsRecorders) {
		this.hitsRecorders = hitsRecorders;
		this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, r -> {
			Thread thread = new Thread(r);
			thread.setName("hits-down-thread");
			return thread;
		});
	}
	
	/**
	 * 开始下发
	 *
	 * @param delay    延迟
	 * @param timeUnit 时间单位
	 */
	public void start(long delay, @NotNull TimeUnit timeUnit) {
		scheduledThreadPoolExecutor.scheduleWithFixedDelay(this::hitsDown, delay, delay, timeUnit);
	}
	
	/**
	 * 下发
	 */
	public void hitsDown() {
		log.debug("hitsDown start");
		for (HitsRecorder<?, ?> hitsRecorder : hitsRecorders) {
			try {
				hitsRecorder.hitsDown();
			} catch (Exception e) {
				log.error("hitsDown发生错误,class:{},message：{}", hitsRecorder.getClass(), e.getMessage(), e);
			}
		}
		log.debug("hitsDown end");
	}
	
	/**
	 * 停止下发
	 */
	public void shutdown() {
		scheduledThreadPoolExecutor.shutdown();
	}
	
	/**
	 * 立即停止下发
	 */
	public void shutdownNow() {
		scheduledThreadPoolExecutor.shutdownNow();
	}
}
