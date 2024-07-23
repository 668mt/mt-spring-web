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
public interface EntityService<EntityDO, EntityVO, EntityDTO, EntityCondition> {
	@Transactional(rollbackFor = Exception.class)
	EntityVO addOrUpdate(@NotNull EntityDTO entityDTO);
	
	PageInfo<EntityVO> findPage(@Nullable EntityCondition condition);
	
	EntityVO findById(@NotNull Long id);
	
	@Transactional(rollbackFor = Exception.class)
	void deletes(@NotNull List<Long> ids);
}
