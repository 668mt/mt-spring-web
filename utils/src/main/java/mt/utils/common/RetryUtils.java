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
		T execute() throws Exception;
		
		default void onException(Exception exception) {
		}
	}
	
	public static <T> T doWithRetry(DoExecute<T> doExecute, int retry, long delayMills, Class<?>... retryExceptions) {
		if (retryExceptions == null) {
			retryExceptions = new Class[]{Exception.class};
		}
		Exception finalException;
		do {
			try {
				return doExecute.execute();
			} catch (Exception e) {
				finalException = e;
				doExecute.onException(e);
				Arrays.stream(retryExceptions)
						.filter(aClass -> aClass.isAssignableFrom(e.getClass()))
						.findFirst()
						.orElseThrow(() -> new RuntimeException(e));
				retry--;
				if (delayMills > 0) {
					try {
						Thread.sleep(delayMills);
					} catch (InterruptedException interruptedException) {
						throw new RuntimeException(interruptedException);
					}
				}
			}
		} while (retry >= 0);
		throw new RuntimeException(finalException);
	}
}
