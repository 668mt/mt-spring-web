package mt.common.progress;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Martin
 * @Date 2023/4/5
 */
public class BufferedProgress implements Progress {
	private final ProgressStore progressStore;
	private final double bufferPercent;
	private final String key;
	private final ProgressListener progressListener;
	private double lastPercent = 0;
	private double currPercent = 0;
	
	public BufferedProgress(@NotNull String key, @NotNull ProgressStore progressStore, double bufferPercent) {
		this(key, progressStore, bufferPercent, null);
	}
	
	public BufferedProgress(@NotNull String key, @NotNull ProgressStore progressStore, double bufferPercent, @Nullable ProgressListener progressListener) {
		this.key = key;
		this.progressStore = progressStore;
		this.bufferPercent = bufferPercent;
		this.progressListener = progressListener;
	}
	
	private void listen() {
		if (progressListener != null) {
			progressListener.listen(getPercent());
		}
	}
	
	@Override
	public void init() {
		progressStore.init(key);
		lastPercent = 0d;
		currPercent = 0d;
		listen();
	}
	
	@Override
	public void update(double percent) {
		if (percent - lastPercent >= bufferPercent || percent >= 0.9999) {
			progressStore.update(key, percent);
			lastPercent = percent;
			listen();
		}
		currPercent = percent;
	}
	
	@Override
	public void add(double addPercent) {
		currPercent += addPercent;
		if (currPercent - lastPercent >= bufferPercent || currPercent >= 0.9999) {
			progressStore.add(key, currPercent - lastPercent);
			lastPercent = currPercent;
			listen();
		}
	}
	
	@Override
	public void remove() {
		progressStore.remove(key);
		lastPercent = 0;
		currPercent = 0;
	}
	
	@Override
	public double getPercent() {
		return progressStore.getPercent(key);
	}
	
	@Override
	public void finish() {
		progressStore.finish(key);
	}
	
	public static void main(String[] args) {
		LocalProgressStore localProgress = new LocalProgressStore();
		AtomicInteger count = new AtomicInteger(0);
		String key = "test";
		BufferedProgress bufferedProgress = new BufferedProgress(key, localProgress, 0.01, (percent) -> {
			System.out.println(percent);
		});
		PartProgress part1 = PartProgress.createPart(bufferedProgress, 0.5);
		PartProgress part2 = PartProgress.createPart(bufferedProgress, 0.5);
		for (int i = 0; i < 10000; i++) {
			part1.add(0.0001);
		}
		for (int i = 0; i < 10000; i++) {
			part2.add(0.0001);
		}
		System.out.println(count.get());
		System.out.println("result:" + bufferedProgress.getPercent());
	}
}
