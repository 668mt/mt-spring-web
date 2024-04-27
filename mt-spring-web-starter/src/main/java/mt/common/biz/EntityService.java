package mt.common.biz;

import com.github.pagehelper.PageInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Author Martin
 * @Date 2024/4/20
 */
public interface EntityService<Entity, EntityDTO, EntityCondition> {
	@Transactional(rollbackFor = Exception.class)
	Entity addOrUpdate(@NotNull EntityDTO entityDTO);
	
	PageInfo<Entity> findPage(@Nullable EntityCondition condition);
	
	Entity findById(Long id);
	
	@Transactional(rollbackFor = Exception.class)
	void deletes(List<Long> ids);
}
