package mt.common.rank;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @Author Martin
 * @Date 2024/6/18
 */
public interface RankService {
	double addScore(@NotNull String key, @NotNull String member, double score);
	
	List<RankMember> getTopMembers(@NotNull String key, int top);
	
	List<RankMember> getMembers(@NotNull String key, int pageNum, int pageSize);
	
	long getTotalMembers(@NotNull String key);
	
	long getRank(@NotNull String key, @NotNull String member);
	
	void clear(@NotNull String key);
}
