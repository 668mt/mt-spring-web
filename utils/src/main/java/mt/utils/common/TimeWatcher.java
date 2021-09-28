package mt.utils.common;

import lombok.extern.slf4j.Slf4j;

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
	
	public void start(String describe) {
		if (describe != null) {
			log.info("开始执行[{}]...", describe);
		}
		lastRecord = start = System.currentTimeMillis();
	}
	
	public long recordFromStart(String describe) {
		return record(start, describe);
	}
	
	public long recordFromLastRecord(String describe) {
		return record(lastRecord, describe);
	}
	
	private long record(long start, String describe) {
		long end = System.currentTimeMillis();
		this.lastRecord = end;
		long time = end - start;
		log.info("执行结束[{}]，用时{}!", describe, TimeUtils.getReadableTime(time, 3));
		return time;
	}
}
