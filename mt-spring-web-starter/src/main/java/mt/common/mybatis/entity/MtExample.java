package mt.common.mybatis.entity;

import lombok.Getter;
import lombok.Setter;
import tk.mybatis.mapper.entity.Example;

/**
 * @Author Martin
 * @Date 2021/9/28
 */
public class MtExample extends Example {
	public MtExample(Class<?> entityClass) {
		super(entityClass);
	}
	
	public MtExample(Class<?> entityClass, boolean exists) {
		super(entityClass, exists);
	}
	
	public MtExample(Class<?> entityClass, boolean exists, boolean notNull) {
		super(entityClass, exists, notNull);
	}
	
	@Getter
	@Setter
	private String resultMap;
}
