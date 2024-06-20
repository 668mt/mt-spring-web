package mt.spring.core.progress;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Martin
 * @Date 2023/4/5
 */
public class BufferedProgress implements Progress {
	private final ProgressService progressService;
	private final double bufferPercent;
	private final String key;
	private final ProgressListener progressListener;
	private double lastPercent = 0;
	private double currPercent = 0;
	
	public BufferedProgress(@NotNull String key, @NotNull ProgressService progressService, double bufferPercent) {
		this(key, progressService, bufferPercent, null);
	}
	
	public BufferedProgress(@NotNull String key, @NotNull ProgressService progressService, double bufferPercent, @Nullable ProgressListener progressListener) {
		this.key = key;
		this.progressService = progressService;
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
		progressService.init(key);
		lastPercent = 0d;
		currPercent = 0d;
		listen();
	}
	
	@Override
	public void update(double percent) {
		if (percent - lastPercent >= bufferPercent || percent >= 0.9999) {
			progressService.update(key, percent);
			lastPercent = percent;
			listen();
		}
		currPercent = percent;
	}
	
	@Override
	public void add(double addPercent) {
		currPercent += addPercent;
		if (currPercent - lastPercent >= bufferPercent || currPercent >= 0.9999) {
			progressService.add(key, currPercent - lastPercent);
			lastPercent = currPercent;
			listen();
		}
	}
	
	@Override
	public void remove() {
		progressService.remove(key);
		lastPercent = 0;
		currPercent = 0;
	}
	
	@Override
	public double getPercent() {
		return progressService.getPercent(key);
	}
	
	@Override
	public void finish() {
		progressService.finish(key);
	}
	
	public static void main(String[] args) {
		LocalProgressService localProgress = new LocalProgressService();
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
