package mt.spring.redis.config;

import cn.hutool.cron.CronUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * @Author Martin
 * @Date 2024/6/23
 */
@Component
public class CronConfig implements InitializingBean {
	@Override
	public void afterPropertiesSet() throws Exception {
		CronUtil.setMatchSecond(true);
		if (!CronUtil.getScheduler().isStarted()) {
			CronUtil.start();
		}
	}
}
