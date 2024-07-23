package mt.common.biz;

import com.github.pagehelper.PageInfo;
import mt.common.entity.PageCondition;
import mt.common.entity.dto.BaseDTO;
import mt.common.entity.po.BaseEntity;
import mt.common.service.BaseService;
import mt.common.tkmapper.Filter;
import mt.common.utils.BeanUtils;
import mt.utils.ClassUtils;
import mt.utils.common.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2024/4/27
 */
public abstract class AbstractEntityService<EntityDO extends BaseEntity, EntityVO, EntityDTO extends BaseDTO, EntityCondition extends PageCondition> implements EntityService<EntityDO, EntityVO, EntityDTO, EntityCondition> {
	private final ApplicationEventPublisher applicationEventPublisher;
	
	public abstract BaseService<EntityDO> getBaseService();
	
	public void beforeAddOrUpdate(@NotNull EntityDO entityDO) {
	
	}
	
	public void beforeDelete(@NotNull EntityDO entityDO) {
		applicationEventPublisher.publishEvent(new EntityDeleteCheckEvent<>(this, entityDO));
	}
	
	public AbstractEntityService(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
	
	@SuppressWarnings("unchecked")
	protected EntityVO transformEntityToVO(@Nullable EntityDO entityDO, @NotNull Class<EntityVO> type) {
		if (entityDO == null) {
			return null;
		}
		if (type.equals(entityDO.getClass())) {
			return (EntityVO) entityDO;
		}
		return BeanUtils.transform(entityDO, type);
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public EntityVO addOrUpdate(@NotNull EntityDTO entityDTO) {
		Long id = entityDTO.getId();
		Class<EntityDO> entityClass = getBaseService().getEntityClass();
		EntityDO entityDO = BeanUtils.transform(entityDTO, entityClass);
		beforeAddOrUpdate(entityDO);
		if (id == null) {
			getBaseService().save(entityDO);
		} else {
			entityDO.setId(id);
			getBaseService().updateByIdSelective(entityDO);
		}
		return transformEntityToVO(entityDO, getEntityVOClass());
	}
	
	@Override
	public PageInfo<EntityVO> findPage(@Nullable EntityCondition entityCondition) {
		PageInfo<EntityDO> pageInfo = getBaseService().findPage(entityCondition);
		PageInfo<EntityVO> targetPageInfo = new PageInfo<>();
		targetPageInfo.setPageNum(pageInfo.getPageNum());
		targetPageInfo.setPageSize(pageInfo.getPageSize());
		targetPageInfo.setTotal(pageInfo.getTotal());
		targetPageInfo.setPages(pageInfo.getPages());
		List<EntityVO> targets = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(pageInfo.getList())) {
			for (EntityDO entityDO : pageInfo.getList()) {
				targets.add(transformEntityToVO(entityDO, getEntityVOClass()));
			}
		}
		targetPageInfo.setList(targets);
		return targetPageInfo;
	}
	
	@Override
	public void deletes(@NotNull List<Long> ids) {
		if (CollectionUtils.isEmpty(ids)) {
			return;
		}
		Filter idsFilter = new Filter("id", Filter.Operator.in, ids);
		List<EntityDO> list = getBaseService().findByFilter(idsFilter);
		if (CollectionUtils.isNotEmpty(list)) {
			for (EntityDO entityDO : list) {
				beforeDelete(entityDO);
			}
			getBaseService().deleteByFilter(idsFilter);
		}
	}
	
	@Override
	public EntityVO findById(@NotNull Long id) {
		EntityDO entityDO = getBaseService().findById(id);
		return transformEntityToVO(entityDO, getEntityVOClass());
	}
	
	/**
	 * 获取EntityVO的class
	 *
	 * @return EntityVO的class
	 */
	@SuppressWarnings("unchecked")
	public Class<EntityVO> getEntityVOClass() {
		ParameterizedType parameterizedType = ClassUtils.findGenericSuperclass(getClass(), AbstractEntityService.class);
		if (parameterizedType != null && parameterizedType.getActualTypeArguments().length == 4) {
			if (parameterizedType.getActualTypeArguments()[1] instanceof Class) {
				return (Class<EntityVO>) parameterizedType.getActualTypeArguments()[1];
			}
		}
		throw new IllegalStateException("can't find entityVOClass");
	}
}
