package mt.common.hits;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2023/4/2
 */
public class LocalHitsRecorder<SCOPE, KEY> implements HitsRecorder<SCOPE, KEY> {
	private final Map<SCOPE, Map<KEY, Long>> localScopedHitsMap = new HashMap<>();
	private final HitsDownHandler<SCOPE, KEY> hitsDownHandler;
	
	public LocalHitsRecorder(@NotNull HitsDownHandler<SCOPE, KEY> hitsDownHandler) {
		this.hitsDownHandler = hitsDownHandler;
	}
	
	@Override
	public void recordHits(@Nullable SCOPE scope, @NotNull KEY key, long hits) {
		Map<KEY, Long> localHitsMap = localScopedHitsMap.computeIfAbsent(scope, k -> new HashMap<>());
		Long cachedHits = localHitsMap.get(key);
		if (cachedHits == null) {
			cachedHits = 0L;
		}
		cachedHits += hits;
		localHitsMap.put(key, cachedHits);
	}
	
	@Override
	public void hitsDown() {
		for (Map.Entry<SCOPE, Map<KEY, Long>> stringMapEntry : localScopedHitsMap.entrySet()) {
			SCOPE scope = stringMapEntry.getKey();
			Map<KEY, Long> localHitsMap = stringMapEntry.getValue();
			if (localHitsMap.size() > 0) {
				hitsDownHandler.doHitsDown(scope, localHitsMap);
				localHitsMap.clear();
			}
		}
	}
}
