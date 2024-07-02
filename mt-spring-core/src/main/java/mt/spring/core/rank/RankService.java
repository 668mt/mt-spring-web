package mt.spring.core.rank;

import com.github.pagehelper.PageInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @Author Martin
 * @Date 2024/6/18
 */
public interface RankService {
	/**
	 * 添加分数
	 *
	 * @param key    key
	 * @param member 成员
	 * @param score  分数
	 * @return 分数
	 */
	double addScore(@NotNull String key, @NotNull String member, double score);
	
	/**
	 * 获取排名
	 *
	 * @param key key
	 * @param top top
	 * @return 成员
	 */
	List<String> getTopMembers(@NotNull String key, int top);
	
	List<RankMember> getTopMembersWithRank(@NotNull String key, int top);
	
	/**
	 * 分页查询获取排名
	 *
	 * @param key
	 * @param pageNum
	 * @param pageSize
	 * @param reverse  false：分数从高到低，true：分数从低到高
	 * @return
	 */
	PageInfo<RankMember> getMembersPage(@NotNull String key, int pageNum, int pageSize, boolean reverse);
	
	/**
	 * 分页查询获取排名，分数从高到低
	 *
	 * @param key      key
	 * @param pageNum  页码
	 * @param pageSize 每页大小
	 * @return 分页结果
	 */
	default PageInfo<RankMember> getMembersPage(@NotNull String key, int pageNum, int pageSize) {
		return getMembersPage(key, pageNum, pageSize, false);
	}
	
	/**
	 * 获取总成员数
	 *
	 * @param key
	 * @return
	 */
	long getTotalMembers(@NotNull String key);
	
	/**
	 * 获取排名
	 *
	 * @param key
	 * @param member
	 * @return
	 */
	long getRank(@NotNull String key, @NotNull String member);
	
	/**
	 * 清空
	 *
	 * @param key
	 */
	void clear(@NotNull String key);
}
