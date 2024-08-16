package test.biz;

import lombok.Data;
import mt.common.biz.AbstractSimpleEntityService;
import mt.common.entity.PageCondition;
import mt.common.entity.dto.BaseDTO;
import mt.common.entity.po.BaseEntity;
import mt.common.service.BaseRepository;

/**
 * @Author Martin
 * @Date 2024/7/23
 */
public class TestService {
	@Data
	public static class User extends BaseEntity {
		private String name;
		private Integer age;
	}
	
	@Data
	public static class UserDTO extends BaseDTO {
		private String name;
		private Integer age;
	}
	
	@Data
	public static class UserCondition extends PageCondition {
		private String name;
		private Integer age;
	}
	
	public static class UserService extends AbstractSimpleEntityService<User, UserDTO, UserCondition> {
		public UserService() {
			super(null);
		}
		
		@Override
		public BaseRepository<User> getBaseService() {
			return null;
		}
	}
	
	public static void main(String[] args) {
		UserService userService = new UserService();
		System.out.println(userService.getEntityVOClass());
	}
}
