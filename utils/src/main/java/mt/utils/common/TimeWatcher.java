package mt.utils.common;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
@Slf4j
public class TimeWatcher {
	private long start;
	private long lastRecord;
	
	public TimeWatcher() {
	}
	
	public void start() {
		start(null);
	}
	
	public void start(@Nullable String taskName) {
		if (taskName != null) {
			log.info("{}开始执行...", taskName);
		}
		lastRecord = start = System.currentTimeMillis();
	}
	
	public long printFromStart(@NotNull String taskName) {
		return printFrom(start, taskName);
	}
	
	public long printFromLastRecord(@NotNull String taskName) {
		return printFrom(lastRecord, taskName);
	}
	
	private long printFrom(long startTime, @NotNull String taskName) {
		long end = System.currentTimeMillis();
		this.lastRecord = end;
		long time = end - startTime;
		log.info("{}执行结束，用时{}!", taskName, TimeUtils.getReadableTime(time, 3));
		return time;
	}
}
