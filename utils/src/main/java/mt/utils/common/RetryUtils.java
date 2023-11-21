package mt.utils.common;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * @Author Martin
 * @Date 2021/4/2
 */
@Slf4j
public class RetryUtils {
	public interface DoExecute<T> {
		T execute() throws Throwable;
		
		default void onException(Throwable exception) {
		}
	}
	
	public static <T> T doWithRetry(DoExecute<T> doExecute, int retry, long delayMills, Class<?>... retryExceptions) {
		if (retryExceptions == null) {
			retryExceptions = new Class[]{Exception.class};
		}
		Throwable finalException;
		do {
			try {
				return doExecute.execute();
			} catch (Throwable e) {
				finalException = e;
				doExecute.onException(e);
				Arrays.stream(retryExceptions)
					.filter(aClass -> aClass.isAssignableFrom(e.getClass()))
					.findFirst()
					.orElseThrow(() -> new RuntimeException(e));
				log.warn("操作失败，重试中，剩余次数：{}", retry - 1);
				if (delayMills > 0) {
					try {
						Thread.sleep(delayMills);
					} catch (InterruptedException interruptedException) {
						throw new RuntimeException(interruptedException);
					}
				}
			} finally {
				retry--;
			}
		} while (retry >= 0);
		throw new RuntimeException(finalException);
	}
}
