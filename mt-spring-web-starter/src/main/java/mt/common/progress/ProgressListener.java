package mt.common.progress;

public interface ProgressListener {
	/**
	 * 监听进度
	 *
	 * @param percent
	 */
	void listen(double percent);
}