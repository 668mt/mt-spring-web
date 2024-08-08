package mt.common.biz;

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.ApiOperation;
import mt.common.entity.ResResult;
import mt.utils.common.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * @Author Martin
 * @Date 2024/4/19
 */
public abstract class BaseController<EntityDO, EntityVO, EntityDTO, EntityCondition> {
	
	protected final EntityService<EntityDO, EntityVO, EntityDTO, EntityCondition> entityService;
	
	public BaseController(EntityService<EntityDO, EntityVO, EntityDTO, EntityCondition> entityService) {
		this.entityService = entityService;
	}
	
	@GetMapping
	@ApiOperation(value = "获取列表")
	public ResResult<PageInfo<EntityVO>> list(EntityCondition condition) {
		return ResResult.success(entityService.findPage(condition));
	}
	
	@PostMapping
	@ApiOperation(value = "新增或修改", notes = "id为空时新增，否则修改")
	public ResResult<EntityVO> addOrUpdate(@RequestBody @Validated EntityDTO entityDTO) {
		return ResResult.success(entityService.addOrUpdate(entityDTO));
	}
	
	@GetMapping("/{id}")
	@ApiOperation(value = "根据id获取")
	public ResResult<EntityVO> findById(@PathVariable Long id) {
		return ResResult.success(entityService.findById(id));
	}
	
	@DeleteMapping("/{id}")
	@ApiOperation(value = "根据id删除")
	public ResResult<Void> delete(@PathVariable Long id) {
		entityService.deletes(Arrays.asList(id));
		return ResResult.success();
	}
	
	@DeleteMapping
	@ApiOperation(value = "批量删除")
	public ResResult<Void> deletes(List<Long> ids) {
		if (CollectionUtils.isNotEmpty(ids)) {
			entityService.deletes(ids);
		}
		return ResResult.success();
	}
}
