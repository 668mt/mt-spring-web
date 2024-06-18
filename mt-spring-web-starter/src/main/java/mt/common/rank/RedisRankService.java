package mt.common.rank;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import mt.common.delayexecute.RedisDelayExecutor;
import mt.common.redis.RedisService;
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
	
	public RedisRankService(String name, RedisService redisService, int expireSeconds) {
		this.redisService = redisService;
		this.expireSeconds = expireSeconds;
		redisDelayExecutor = new RedisDelayExecutor(redisService, "rank-delay-exe:" + name, json -> {
			JSONObject params = JSONObject.parseObject(json);
			String key = params.getString("key");
			String member = params.getString("member");
			double v = addScore(key, member, -1);
			if (v <= 0) {
				redisService.getRedisTemplate().opsForZSet().remove(key, member);
			}
		});
	}
	
	@Override
	public double addScore(@NotNull String key, @NotNull String member, double score) {
		Double r = redisService.getRedisTemplate().opsForZSet().incrementScore(key, member, score);
		//一段时间内的排行榜
		//延迟执行
		JSONObject params = new JSONObject();
		params.put("key", key);
		params.put("member", member);
		params.put("uid", UUID.randomUUID().toString());
		redisDelayExecutor.register(params.toJSONString(), System.currentTimeMillis() + expireSeconds * 1000L);
		return r == null ? -1 : r;
	}
	
	@Override
	public long getTotalMembers(@NotNull String key) {
		Long size = redisService.getRedisTemplate().opsForZSet().size(key);
		return size == null ? 0 : size;
	}
	
	@Override
	public long getRank(@NotNull String key, @NotNull String member) {
		Long rank = redisService.getRedisTemplate().opsForZSet().rank(key, member);
		return rank == null ? -1 : rank;
	}
	
	@Override
	public List<RankMember> getTopMembers(@NotNull String key, int top) {
		return getMembers(key, 1, top);
	}
	
	@Override
	public List<RankMember> getMembers(@NotNull String key, int pageNum, int pageSize) {
		int start = (pageNum - 1) * pageSize;
		int end = start + pageSize - 1;
		Set<ZSetOperations.TypedTuple<Object>> typedTuples = redisService.getRedisTemplate().opsForZSet().reverseRangeWithScores(key, start, end);
		if (CollectionUtils.isEmpty(typedTuples)) {
			return new ArrayList<>();
		}
		return typedTuples.stream()
			.filter(objectTypedTuple -> objectTypedTuple.getValue() != null)
			.map(typedTuple -> new RankMember(typedTuple.getValue().toString(), typedTuple.getScore()))
			.collect(Collectors.toList());
	}
	
	@Override
	public void clear(@NotNull String key) {
		redisDelayExecutor.clear();
		redisService.getRedisTemplate().delete(key);
	}
	
}
