package mt.common.config;

import mt.common.entity.DataLock;
import mt.common.service.DataLockService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @Author Martin
 * @Date 2022/8/13
 */
public class DataLockConfiguration {
	@Bean
	@ConditionalOnMissingBean(DataLockService.class)
	public DataLockService dataLockService(CommonProperties commonProperties) {
		SystemEntity.register(DataLock.class);
		return new DataLockService(commonProperties);
	}
}
