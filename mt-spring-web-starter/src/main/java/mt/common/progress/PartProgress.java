package mt.common.progress;

import org.jetbrains.annotations.NotNull;

/**
 * @Author Martin
 * @Date 2023/4/5
 */
public class PartProgress implements Progress {
	private final Progress delegate;
	private final double partPercent;
	private double lastPercent = 0;
	
	public PartProgress(@NotNull Progress delegate, double partPercent) {
		this.delegate = delegate;
		this.partPercent = partPercent;
	}
	
	/**
	 * 创建一个子进度
	 *
	 * @param progress    进度
	 * @param partPercent 子进度占比
	 * @return 子进度
	 */
	public static PartProgress createPart(Progress progress, double partPercent) {
		return new PartProgress(progress, partPercent);
	}
	
	@Override
	public void update(double percent) {
		double addPercent = percent - lastPercent;
		delegate.add(addPercent * partPercent);
		this.lastPercent = percent;
	}
	
	@Override
	public void add(double percent) {
		delegate.add(percent * partPercent);
	}
	
	@Override
	public void remove() {
		//不支持移除
	}
	
	@Override
	public double getPercent() {
		//不支持获取
		return 0d;
	}
	
	public static void main(String[] args) {
		String key = "test";
		LocalProgressStore progress = new LocalProgressStore();
		BufferedProgress bufferedProgress = new BufferedProgress(key, progress, 0.01);
		PartProgress part1 = PartProgress.createPart(bufferedProgress, 0.5);
		PartProgress part2 = PartProgress.createPart(bufferedProgress, 0.5);
		for (double percent = 0; percent <= 1; percent += 0.01) {
			part1.update(percent);
		}
		System.out.println(progress.getPercent(key));
	}
}
