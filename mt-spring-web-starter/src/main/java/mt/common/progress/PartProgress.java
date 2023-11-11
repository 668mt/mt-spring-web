package mt.common.progress;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
	public void update(@NotNull String key, double percent) {
		double addPercent = percent - lastPercent;
		delegate.add(key, addPercent * partPercent);
		this.lastPercent = percent;
	}
	
	private double scale(double value) {
		return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
	
	@Override
	public void add(@NotNull String key, double percent) {
		delegate.add(key, percent * partPercent);
	}
	
	@Override
	public void remove(@NotNull String key) {
		//不支持移除
	}
	
	@Override
	public double getPercent(@NotNull String key) {
		//不支持获取
		return 0d;
	}
	
	public static void main(String[] args) {
		String key = "test";
		LocalProgress progress = new LocalProgress();
		PartProgress part1 = PartProgress.createPart(progress, 0.5);
		PartProgress part2 = PartProgress.createPart(progress, 0.5);
		for (double percent = 0; percent <= 1; percent += 0.01) {
			part1.update(key, percent);
		}
		System.out.println(progress.getPercent(key));
	}
}
