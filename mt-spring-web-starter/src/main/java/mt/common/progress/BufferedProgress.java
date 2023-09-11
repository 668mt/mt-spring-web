//package mt.common.progress;
//
//import org.jetbrains.annotations.NotNull;
//
///**
// * @Author Martin
// * @Date 2023/4/5
// */
//public class BufferedProgress implements Progress {
//	private final Progress delegate;
//	private final double bufferPercent;
//	private double lastPercent = 0;
//	private double currPercent = 0;
//
//	public BufferedProgress(@NotNull Progress delegate, double bufferPercent) {
//		this.delegate = delegate;
//		this.bufferPercent = bufferPercent;
//	}
//
//	@Override
//	public void init(@NotNull String key) {
//		delegate.init(key);
//		lastPercent = 0d;
//	}
//
//	@Override
//	public void update(@NotNull String key, double percent) {
//		if (percent - lastPercent >= bufferPercent) {
//			delegate.update(key, percent);
//			lastPercent = percent;
//		}
//		currPercent = percent;
//	}
//
//	@Override
//	public void add(@NotNull String key, double addPercent) {
//		currPercent += addPercent;
//		if (currPercent - lastPercent >= bufferPercent) {
//			delegate.add(key, currPercent - lastPercent);
//			lastPercent = currPercent;
//		}
//	}
//
//	@Override
//	public void remove(@NotNull String key) {
//		delegate.remove(key);
//		lastPercent = 0;
//		currPercent = 0;
//	}
//
//	@Override
//	public double getPercent(@NotNull String key) {
//		return delegate.getPercent(key);
//	}
//
//	@Override
//	public void finish(@NotNull String key) {
//		delegate.finish(key);
//	}
//}
