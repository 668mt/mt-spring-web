package mt.spring.core.hits;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author Martin
 * @Date 2023/4/2
 */
public class LocalHitsRecorder<SCOPE, KEY> implements HitsRecorder<SCOPE, KEY> {
	private Map<SCOPE, ConcurrentHashMap<KEY, AtomicLong>> localScopedHitsMap = new ConcurrentHashMap<>(16);
	private final HitsDownHandler<SCOPE, KEY> hitsDownHandler;
	
	public LocalHitsRecorder(@NotNull HitsDownHandler<SCOPE, KEY> hitsDownHandler) {
		this.hitsDownHandler = hitsDownHandler;
	}
	
	@Override
	public void recordHits(@NotNull SCOPE scope, @NotNull KEY key, long hits) {
		localScopedHitsMap.computeIfAbsent(scope, k -> new ConcurrentHashMap<>(16))
			.computeIfAbsent(key, k -> new AtomicLong(0))
			.getAndAdd(hits);
	}
	
	@Override
	public void hitsDown() {
		for (Map.Entry<SCOPE, ConcurrentHashMap<KEY, AtomicLong>> entry : localScopedHitsMap.entrySet()) {
			SCOPE scope = entry.getKey();
			Map<KEY, Long> clickMap = new ConcurrentHashMap<>();
			ConcurrentHashMap<KEY, AtomicLong> scopedClicks = entry.getValue();
			for (Map.Entry<KEY, AtomicLong> keyLongAdderEntry : scopedClicks.entrySet()) {
				long hits = keyLongAdderEntry.getValue().getAndSet(0);
				// 获取最后一次的点击量
				clickMap.put(keyLongAdderEntry.getKey(), hits);
			}
			hitsDownHandler.doHitsDown(scope, clickMap);
		}
	}
}
