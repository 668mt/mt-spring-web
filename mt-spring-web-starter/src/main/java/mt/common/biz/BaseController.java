package mt.common.biz;

import com.github.pagehelper.PageInfo;
import mt.common.entity.ResResult;
import mt.utils.common.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author Martin
 * @Date 2024/4/19
 */
public abstract class BaseController<Entity, EntityDTO, EntityCondition> {
	
	protected final EntityService<Entity, EntityDTO, EntityCondition> entityService;
	
	public BaseController(EntityService<Entity, EntityDTO, EntityCondition> entityService) {
		this.entityService = entityService;
	}
	
	@GetMapping
	public ResResult<PageInfo<Entity>> list(EntityCondition condition) {
		return ResResult.success(entityService.findPage(condition));
	}
	
	@PostMapping
	public ResResult<Entity> addOrUpdate(@RequestBody @Validated EntityDTO entityDTO) {
		return ResResult.success(entityService.addOrUpdate(entityDTO));
	}
	
	@GetMapping("/{id}")
	public ResResult<Entity> findById(@PathVariable Long id) {
		return ResResult.success(entityService.findById(id));
	}
	
	@DeleteMapping("/{id}")
	public ResResult<Void> delete(@PathVariable Long id) {
		entityService.deletes(List.of(id));
		return ResResult.success();
	}
	
	@DeleteMapping
	public ResResult<Void> deletes(List<Long> ids) {
		if (CollectionUtils.isNotEmpty(ids)) {
			entityService.deletes(ids);
		}
		return ResResult.success();
	}
}
