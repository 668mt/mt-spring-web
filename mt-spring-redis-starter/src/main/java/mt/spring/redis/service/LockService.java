package mt.spring.redis.service;

import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2020/12/17
 */
public record LockService(RedissonClient redissonClient) {
	
	public RedissonClient getRedissonClient() {
		return redissonClient;
	}
	
	public interface LockCallbackWithResult<T> {
		T afterLocked();
	}
	
	public interface LockCallback {
		void afterLocked();
	}
	
	public <T> T doWithLock(String key, LockType lockType, int lockMinutes, LockCallbackWithResult<T> lockCallbackWithResult) {
		RLock lock = null;
		try {
			RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(key);
			lock = lockType == LockType.READ ? readWriteLock.readLock() : readWriteLock.writeLock();
			lock.lock(lockMinutes, TimeUnit.MINUTES);
			return lockCallbackWithResult.afterLocked();
		} finally {
			if (lock != null && lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}
	
	public boolean tryLock(String key, LockCallback lockCallback) {
		RLock lock = redissonClient.getLock(key);
		if (lock.tryLock()) {
			try {
				lockCallback.afterLocked();
				return true;
			} finally {
				if (lock.isHeldByCurrentThread()) {
					lock.unlock();
				}
			}
		} else {
			return false;
		}
	}
	
}
