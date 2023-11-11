package mt.common.progress;

import org.jetbrains.annotations.NotNull;

/**
 * @Author Martin
 * @Date 2023/11/11
 */
public abstract class ProgressListener implements Progress {
	private final Progress proxy;
	
	public ProgressListener(Progress proxy) {
		this.proxy = proxy;
	}
	
	/**
	 * 监听进度
	 *
	 * @param key     进度key
	 * @param percent 进度百分比
	 */
	public abstract void listen(@NotNull String key, double percent);
	
	@Override
	public void update(@NotNull String key, double percent) {
		proxy.update(key, percent);
		listen(key, getPercent(key));
	}
	
	@Override
	public void add(@NotNull String key, double percent) {
		proxy.add(key, percent);
		listen(key, getPercent(key));
	}
	
	@Override
	public void remove(@NotNull String key) {
		proxy.remove(key);
	}
	
	@Override
	public double getPercent(@NotNull String key) {
		return proxy.getPercent(key);
	}
}
