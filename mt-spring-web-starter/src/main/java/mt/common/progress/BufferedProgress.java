package mt.common.progress;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Martin
 * @Date 2023/4/5
 */
public class BufferedProgress implements Progress {
	private final Progress delegate;
	private final double bufferPercent;
	private double lastPercent = 0;
	private double currPercent = 0;
	private ProgressListener progressListener;
	
	public BufferedProgress(@NotNull Progress delegate, double bufferPercent) {
		this(delegate, bufferPercent, null);
	}
	
	public BufferedProgress(@NotNull Progress delegate, double bufferPercent, @Nullable ProgressListener progressListener) {
		this.delegate = delegate;
		this.bufferPercent = bufferPercent;
		this.progressListener = progressListener;
	}
	
	private void listen(@NotNull String key) {
		if (progressListener != null) {
			progressListener.listen(key, getPercent(key));
		}
	}
	
	@Override
	public void init(@NotNull String key) {
		delegate.init(key);
		lastPercent = 0d;
		listen(key);
	}
	
	@Override
	public void update(@NotNull String key, double percent) {
		if (percent - lastPercent >= bufferPercent || percent >= 0.9999) {
			delegate.update(key, percent);
			lastPercent = percent;
			listen(key);
		}
		currPercent = percent;
	}
	
	@Override
	public void add(@NotNull String key, double addPercent) {
		currPercent += addPercent;
		if (currPercent - lastPercent >= bufferPercent || currPercent >= 0.9999) {
			delegate.add(key, currPercent - lastPercent);
			lastPercent = currPercent;
			listen(key);
		}
	}
	
	@Override
	public void remove(@NotNull String key) {
		delegate.remove(key);
		lastPercent = 0;
		currPercent = 0;
	}
	
	@Override
	public double getPercent(@NotNull String key) {
		return delegate.getPercent(key);
	}
	
	@Override
	public void finish(@NotNull String key) {
		delegate.finish(key);
	}
	
	public static void main(String[] args) {
		LocalProgress localProgress = new LocalProgress();
		AtomicInteger count = new AtomicInteger(0);
		BufferedProgress bufferedProgress = new BufferedProgress(localProgress, 0.01, (key, percent) -> {
			System.out.println(percent);
		});
		String key = "test";
		PartProgress part1 = PartProgress.createPart(bufferedProgress, 0.5);
		PartProgress part2 = PartProgress.createPart(bufferedProgress, 0.5);
		for (int i = 0; i < 10000; i++) {
			part1.add(key, 0.0001);
		}
		for (int i = 0; i < 10000; i++) {
			part2.add(key, 0.0001);
		}
		System.out.println(count.get());
		System.out.println("result:" + bufferedProgress.getPercent(key));
	}
}
