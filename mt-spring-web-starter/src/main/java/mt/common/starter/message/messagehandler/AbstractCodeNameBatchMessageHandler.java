package mt.common.starter.message.messagehandler;

import mt.common.service.BaseRepository;
import mt.common.tkmapper.Filter;
import mt.common.tkmapper.Operator;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;

/**
 * @Author Martin
 * @Date 2024/4/27
 */
public abstract class AbstractCodeNameBatchMessageHandler<Entity, CodeType, NameType> implements BatchMessageHandler<CodeType, NameType> {
	protected abstract BaseRepository<Entity> getRepository();
	
	protected abstract NameType getName(Entity entity);
	
	protected abstract CodeType getCode(Entity entity);
	
	protected abstract String getCodeField();
	
	@Override
	public Map<CodeType, NameType> handle(Collection<?> collection, Set<CodeType> codes, String[] params) {
		Map<CodeType, NameType> map = new HashMap<>();
		if (CollectionUtils.isNotEmpty(codes)) {
			List<Entity> list = getRepository().findByFilter(new Filter(getCodeField(), Operator.in, codes));
			if (CollectionUtils.isNotEmpty(list)) {
				for (Entity t : list) {
					map.put(getCode(t), getName(t));
				}
			}
		}
		return map;
	}
}
