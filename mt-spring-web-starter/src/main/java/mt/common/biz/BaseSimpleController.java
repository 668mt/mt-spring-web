package mt.common.biz;

/**
 * @Author Martin
 * @Date 2024/7/23
 */
public abstract class BaseSimpleController<EntityDO, EntityDTO, EntityCondition> extends BaseController<EntityDO, EntityDO, EntityDTO, EntityCondition> {
	public BaseSimpleController(EntityService<EntityDO, EntityDO, EntityDTO, EntityCondition> entityService) {
		super(entityService);
	}
}
