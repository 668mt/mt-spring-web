package mt.common.biz;

import com.github.pagehelper.PageInfo;
import mt.common.entity.PageCondition;
import mt.common.entity.dto.BaseDTO;
import mt.common.entity.po.BaseEntity;
import mt.common.service.BaseService;
import mt.common.tkmapper.Filter;
import mt.common.utils.BeanUtils;
import mt.utils.common.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Author Martin
 * @Date 2024/4/27
 */
public abstract class AbstractEntityService<Entity extends BaseEntity, EntityDTO extends BaseDTO, EntityCondition extends PageCondition> implements EntityService<Entity, EntityDTO, EntityCondition> {
	private final ApplicationEventPublisher applicationEventPublisher;
	
	public abstract BaseService<Entity> getBaseService();
	
	public abstract void beforeAddOrUpdate(Entity entity);
	
	public AbstractEntityService(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
	
	public void beforeDelete(Entity entity) {
		applicationEventPublisher.publishEvent(new EntityDeleteCheckEvent<>(this, entity));
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Entity addOrUpdate(@NotNull EntityDTO entityDTO) {
		Long id = entityDTO.getId();
		Class<Entity> entityClass = getBaseService().getEntityClass();
		Entity entity = BeanUtils.transform(entityDTO, entityClass);
		beforeAddOrUpdate(entity);
		if (id == null) {
			getBaseService().save(entity);
		} else {
			entity.setId(id);
			getBaseService().updateByIdSelective(entity);
		}
		return entity;
	}
	
	@Override
	public PageInfo<Entity> findPage(@Nullable EntityCondition entityCondition) {
		return getBaseService().findPage(entityCondition);
	}
	
	@Override
	public void deletes(List<Long> ids) {
		if (CollectionUtils.isEmpty(ids)) {
			return;
		}
		Filter idsFilter = new Filter("id", Filter.Operator.in, ids);
		List<Entity> list = getBaseService().findByFilter(idsFilter);
		if (CollectionUtils.isNotEmpty(list)) {
			for (Entity entity : list) {
				beforeDelete(entity);
			}
			getBaseService().deleteByFilter(idsFilter);
		}
	}
	
	@Override
	public Entity findById(Long id) {
		return getBaseService().findById(id);
	}
}
