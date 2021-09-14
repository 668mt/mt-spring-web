package mt.common.starter.message.messagehandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Martin
 * @Date 2020-04-30
 */
public abstract class AbstractCacheMessageHandler<Entity, T> implements MessageHandler<Entity, T> {
	protected Map<String, T> cache = new HashMap<>();
	
	public void clearCache() {
		cache.clear();
	}
	
	@Override
	public void init() {
		clearCache();
	}
	
	@Override
	public T handle(Entity entity, Object[] params, String mark) {
		return getValueUseCacheOrNoCache(entity, params, mark);
	}
	
	public abstract String getCacheKey(Entity entity, Object[] params, String mark);
	
	public abstract T getNoCacheValue(Entity entity, Object[] params, String mark);
	
	public T getValueUseCacheOrNoCache(Entity entity, Object[] params, String mark) {
		String cacheKey = getCacheKey(entity, params, mark);
		T o = cache.get(cacheKey);
		if (o != null) {
			return o;
		} else {
			o = getNoCacheValue(entity, params, mark);
			cache.put(cacheKey, o);
			return o;
		}
	}
	
}
