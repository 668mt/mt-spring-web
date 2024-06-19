package mt.spring.core.progress;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2023/4/5
 */
public class LocalProgressStore implements ProgressStore {
	private final Map<String, Double> progressMap = new HashMap<>(16);
	
	@Override
	public void update(@NotNull String key, double percent) {
		progressMap.put(key, percent);
	}
	
	@Override
	public void add(@NotNull String key, double percent) {
		synchronized (this) {
			Double curr = progressMap.get(key);
			if (curr == null) {
				curr = 0d;
			}
			percent += curr;
			progressMap.put(key, percent);
		}
	}
	
	@Override
	public void remove(@NotNull String key) {
		progressMap.remove(key);
	}
	
	@Override
	public double getPercent(@NotNull String key) {
		Double percent = progressMap.get(key);
		percent = percent == null ? 0 : percent;
		return BigDecimal.valueOf(percent).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
}
