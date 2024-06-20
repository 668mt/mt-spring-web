package mt.spring.redis.service;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import lombok.Getter;
import mt.spring.core.rank.RankMember;
import mt.spring.core.rank.RankService;
import mt.utils.common.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2024/6/18
 */
public class RedisRankService implements RankService {
	private final RedisService redisService;
	private final int expireSeconds;
	@Getter
	private final RedisDelayExecutor redisDelayExecutor;
	
	public RedisRankService(RedisService redisService, String name, int expireSeconds) {
		this.redisService = redisService;
		this.expireSeconds = expireSeconds;
		redisDelayExecutor = new RedisDelayExecutor(redisService, "rank-delay-exe-" + name, json -> {
			JSONObject params = JSONObject.parseObject(json);
			String key = params.getString("key");
			String member = params.getString("member");
			double v = addScore(key, member, -1);
			if (v <= 0) {
				redisService.getRedisTemplate().opsForZSet().remove(key, member);
			}
		});
	}
	
	private String getKey(String key) {
		return redisService.getRedisPrefix() + ":" + key;
	}
	
	@Override
	public double addScore(@NotNull String key, @NotNull String member, double score) {
		Double r = redisService.getRedisTemplate().opsForZSet().incrementScore(getKey(key), member, score);
		//一段时间内的排行榜
		//延迟执行
		JSONObject params = new JSONObject();
		params.put("key", getKey(key));
		params.put("member", member);
		params.put("uid", UUID.randomUUID().toString());
		redisDelayExecutor.register(params.toJSONString(), System.currentTimeMillis() + expireSeconds * 1000L);
		return r == null ? -1 : r;
	}
	
	@Override
	public long getTotalMembers(@NotNull String key) {
		Long size = redisService.getRedisTemplate().opsForZSet().size(getKey(key));
		return size == null ? 0 : size;
	}
	
	@Override
	public long getRank(@NotNull String key, @NotNull String member) {
		Long rank = redisService.getRedisTemplate().opsForZSet().rank(getKey(key), member);
		return rank == null ? -1 : rank;
	}
	
	@Override
	public List<RankMember> getTopMembers(@NotNull String key, int top) {
		PageInfo<RankMember> page = getMembersPage(key, 1, top, false, false);
		return page.getList();
	}
	
	@Override
	public PageInfo<RankMember> getMembersPage(@NotNull String key, int pageNum, int pageSize, boolean reverse) {
		return getMembersPage(key, pageNum, pageSize, reverse, true);
	}
	
	private PageInfo<RankMember> getMembersPage(@NotNull String key, int pageNum, int pageSize, boolean reverse, boolean getTotal) {
		key = getKey(key);
		int start = (pageNum - 1) * pageSize;
		int end = start + pageSize - 1;
		Set<ZSetOperations.TypedTuple<Object>> typedTuples;
		if (reverse) {
			//分数从低到高
			typedTuples = redisService.getRedisTemplate().opsForZSet().rangeWithScores(key, start, end);
		} else {
			//分数从高到低
			typedTuples = redisService.getRedisTemplate().opsForZSet().reverseRangeWithScores(key, start, end);
		}
		PageInfo<RankMember> pageInfo = new PageInfo<>();
		List<RankMember> list;
		long total;
		if (CollectionUtils.isEmpty(typedTuples)) {
			total = 0L;
			list = new ArrayList<>();
		} else {
			list = typedTuples.stream()
				.filter(objectTypedTuple -> objectTypedTuple.getValue() != null)
				.map(typedTuple -> new RankMember(typedTuple.getValue().toString(), typedTuple.getScore()))
				.collect(Collectors.toList());
			if (getTotal) {
				total = getTotalMembers(key);
			} else {
				total = list.size();
			}
		}
		pageInfo.setPageNum(pageNum);
		pageInfo.setPageSize(pageSize);
		pageInfo.setList(list);
		pageInfo.setTotal(total);
		pageInfo.calcByNavigatePages(8);
		return pageInfo;
	}
	
	@Override
	public void clear(@NotNull String key) {
		redisDelayExecutor.clear();
		redisService.getRedisTemplate().delete(getKey(key));
	}
	
}
