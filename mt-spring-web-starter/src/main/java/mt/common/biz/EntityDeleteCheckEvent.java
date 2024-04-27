package mt.common.biz;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @Author Martin
 * @Date 2024/4/27
 */
@Getter
public class EntityDeleteCheckEvent<T> extends ApplicationEvent {
	private final T entity;
	
	public EntityDeleteCheckEvent(Object source, T entity) {
		super(source);
		this.entity = entity;
	}
}
