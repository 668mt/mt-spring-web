package test.messager;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import mt.common.config.CommonProperties;
import mt.common.entity.ResResult;
import mt.common.starter.message.annotation.BatchMessage;
import mt.common.starter.message.messagehandler.BatchMessageHandler;
import mt.common.starter.message.messagehandler.MessageHandler;
import mt.common.starter.message.utils.MessageUtils;
import mt.utils.JsonUtils;
import mt.utils.common.Assert;
import mt.utils.common.CollectionUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

/**
 * @Author Martin
 * @Date 2024/5/26
 */
public class TestMessage {
	@Data
	public static class UserVo {
		private Long id;
		private String username;
		@BatchMessage(column = "id", handlerClass = UserRoleBatchMessageHandler.class)
		private List<RoleVo> roles;
	}
	
	@Data
	public static class RoleVo {
		private String roleName;
	}
	
	public static class UserRoleBatchMessageHandler implements BatchMessageHandler<Long, List<RoleVo>> {
		@Override
		public Map<Long, List<RoleVo>> handle(Collection<?> collection, Set<Long> userIds, String[] params) {
			Map<Long, List<RoleVo>> map = new HashMap<>();
			RoleVo member = new RoleVo();
			member.setRoleName("member");
			RoleVo admin = new RoleVo();
			admin.setRoleName("admin");
			map.put(1L, Arrays.asList(member, admin));
			return map;
		}
	}
	
	private MessageUtils messageUtils;
	
	@Before
	public void init() {
		Map<String, MessageHandler> messageHandlerMap = new HashMap<>();
		messageHandlerMap.put(UserRoleBatchMessageHandler.class.getName(), new UserRoleBatchMessageHandler());
		this.messageUtils = new MessageUtils(new CommonProperties(), messageHandlerMap);
	}
	
	@SuppressWarnings({"rawtypes"})
	@Test
	public void testBatch() {
		ResResult<UserVo> resResult = new ResResult<>();
		UserVo user = new UserVo();
		user.setId(1L);
		resResult.setResult(user);
		messageUtils.message(resResult);
		Assert.state(CollectionUtils.isNotEmpty(resResult.getResult().getRoles()) && resResult.getResult().getRoles().size() == 2, "roles size error");
	}
	
	@Test
	public void testBatch2() {
		ResResult<List<UserVo>> resResult = new ResResult<>();
		UserVo user = new UserVo();
		user.setId(1L);
		UserVo user2 = new UserVo();
		user2.setId(2L);
		resResult.setResult(List.of(user, user2));
		messageUtils.message(resResult);
		System.out.println(JsonUtils.toPrettyJson(resResult));
	}
	
	@Test
	public void testCost() {
		testBatch();
		long start = System.currentTimeMillis();
		try {
			for (int i = 0; i < 10000; i++) {
				testBatch();
			}
		} finally {
			System.out.println("cost:" + (System.currentTimeMillis() - start));
		}
	}
}
