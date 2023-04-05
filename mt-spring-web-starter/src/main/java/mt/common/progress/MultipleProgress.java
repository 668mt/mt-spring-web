//package mt.common.progress;
//
//import mt.utils.common.Assert;
//import org.jetbrains.annotations.NotNull;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * @Author Martin
// * @Date 2023/4/5
// */
//public class MultipleProgress {
//	private final Progress progress;
//
//	public MultipleProgress(@NotNull Progress progress) {
//		this.progress = progress;
//	}
//
//	/**
//	 * partId -> 权重
//	 */
//	private final Map<String, Integer> parts = new HashMap<>(16);
//
//	/**
//	 * 添加part
//	 *
//	 * @param partId partId
//	 * @param weight 权重
//	 */
//	public void addPart(@NotNull String partId, int weight) {
//		Assert.state(weight > 0, "weight必须大于0");
//		parts.put(partId, weight);
//	}
//
//	/**
//	 * 更新进度
//	 *
//	 * @param partId  partId
//	 * @param percent 进度
//	 */
//	public void updatePartPercent(@NotNull String key, @NotNull String partId, double percent) {
//		Integer partWeight = parts.get(partId);
//		Assert.notNull(partWeight, "part不存在:" + partId);
//		progress.update(getPartKey(key, partId), percent);
//	}
//
//	private static String getPartKey(@NotNull String key, @NotNull String partId) {
//		return "multipleProgress:" + key + ":part-" + partId;
//	}
//
//	/**
//	 * 获取进度
//	 *
//	 * @return 进度
//	 */
//	public double getPercent(@NotNull String key) {
//		if (parts.size() == 0) {
//			return 0;
//		}
//		int totalWeight = 0;
//		for (Integer weight : parts.values()) {
//			totalWeight += weight;
//		}
//		double totalPercent = 0;
//		for (Map.Entry<String, Integer> stringWeightEntry : parts.entrySet()) {
//			String partId = stringWeightEntry.getKey();
//			Integer weight = stringWeightEntry.getValue();
//			//占比
//			BigDecimal ratio = BigDecimal.valueOf(weight).divide(BigDecimal.valueOf(totalWeight), 4, RoundingMode.HALF_UP);
//			//当前进度
//			double percent = progress.getPercent(getPartKey(key, partId));
//			totalPercent += percent * ratio.doubleValue();
//		}
//		return BigDecimal.valueOf(totalPercent).setScale(2, RoundingMode.HALF_UP).doubleValue();
//	}
//
//	/**
//	 * 移除进度
//	 *
//	 * @param key 进度key
//	 */
//	public void remove(@NotNull String key) {
//		for (String partId : parts.keySet()) {
//			progress.remove(getPartKey(key, partId));
//		}
//		parts.clear();
//	}
//
//	/**
//	 * 获取part
//	 *
//	 * @param partId partId
//	 * @return part progress
//	 */
//	public Progress getPart(@NotNull String partId) {
//		return new PartProgress(partId, progress);
//	}
//
//	private static class PartProgress implements Progress {
//		private final Progress delegate;
//		private final String partId;
//
//		private String getPartKey(@NotNull String key) {
//			return MultipleProgress.getPartKey(key, partId);
//		}
//
//		public PartProgress(@NotNull String partId, @NotNull Progress delegate) {
//			this.delegate = delegate;
//			this.partId = partId;
//		}
//
//		@Override
//		public void init(@NotNull String key) {
//			delegate.init(getPartKey(key));
//		}
//
//		@Override
//		public void update(@NotNull String key, double percent) {
//			delegate.update(getPartKey(key), percent);
//		}
//
//		@Override
//		public void add(@NotNull String key, double percent) {
//			delegate.add(key, percent);
//		}
//
//		@Override
//		public void remove(@NotNull String key) {
//			delegate.remove(getPartKey(key));
//		}
//
//		@Override
//		public double getPercent(@NotNull String key) {
//			return delegate.getPercent(getPartKey(key));
//		}
//
//		@Override
//		public void finish(@NotNull String key) {
//			delegate.finish(getPartKey(key));
//		}
//	}
//
//	public static void main(String[] args) {
//		double key = new MultipleProgress(new LocalProgress()).getPercent("key");
//	}
//}
