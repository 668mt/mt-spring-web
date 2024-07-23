package mt.common.biz;

import mt.common.entity.PageCondition;
import mt.common.entity.dto.BaseDTO;
import mt.common.entity.po.BaseEntity;
import mt.utils.ClassUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.ParameterizedType;

/**
 * @Author Martin
 * @Date 2024/7/23
 */
public abstract class AbstractSimpleEntityService<EntityDO extends BaseEntity, EntityDTO extends BaseDTO, EntityCondition extends PageCondition> extends AbstractEntityService<EntityDO, EntityDO, EntityDTO, EntityCondition> {
	public AbstractSimpleEntityService(ApplicationEventPublisher applicationEventPublisher) {
		super(applicationEventPublisher);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Class<EntityDO> getEntityVOClass() {
		ParameterizedType parameterizedType = ClassUtils.findGenericSuperclass(getClass(), AbstractSimpleEntityService.class);
		if (parameterizedType != null && parameterizedType.getActualTypeArguments().length == 3) {
			return (Class<EntityDO>) parameterizedType.getActualTypeArguments()[0];
		}
		return super.getEntityVOClass();
	}
}
