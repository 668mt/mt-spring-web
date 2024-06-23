package test.rank;

import mt.spring.core.delayexecute.DelayExecutor;
import mt.spring.core.rank.RankMember;
import mt.spring.redis.service.RedisDelayExecutor;
import mt.spring.redis.service.RedisRankService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import test.redis.AbstractRedisTest;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2024/6/18
 */
public class TestRank extends AbstractRedisTest {
	String rankKey = "rankTest";
	RedisRankService rankService;
	
	@Before
	public void before() {
		RedisDelayExecutor redisDelayExecutor = new RedisDelayExecutor(redisService);
		rankService = new RedisRankService("test", redisService, redisDelayExecutor, 2, TimeUnit.SECONDS);
		rankService.clear(rankKey);
	}
	
	@Test
	public void test() throws InterruptedException {
		int top = 2;
		long totalMembers = rankService.getTotalMembers(rankKey);
		Assert.assertEquals(0, totalMembers);
		rankService.addScore(rankKey, "张三", 1);
		rankService.addScore(rankKey, "李四", 1);
		rankService.addScore(rankKey, "张三", 1);
		rankService.addScore(rankKey, "李四", 1);
		rankService.addScore(rankKey, "王五", 1);
		rankService.addScore(rankKey, "张三", 1);
		rankService.addScore(rankKey, "刘五", 1);
		rankService.addScore(rankKey, "刘五", 1);
		List<RankMember> topMembers = rankService.getTopMembers(rankKey, top);
		System.out.println(topMembers);
		Assert.assertEquals(top, topMembers.size());
		Assert.assertEquals(topMembers.get(0).getMember(), "张三");
		Assert.assertEquals(0, topMembers.get(0).getScore().compareTo(3.0));
		System.out.println(rankService.getMembersPage(rankKey, 2, top));
		Thread.sleep(2000);
		RedisDelayExecutor delayExecutor = (RedisDelayExecutor) rankService.getDelayExecutor();
		delayExecutor.pull();
		System.out.println(rankService.getTopMembers(rankKey, top));
	}
	
	@After
	public void after() {
		rankService.clear(rankKey);
	}
}
