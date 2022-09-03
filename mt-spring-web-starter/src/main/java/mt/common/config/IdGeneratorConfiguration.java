package mt.common.config;

import mt.common.entity.IdGenerate;
import mt.common.mybatis.event.AfterInitEvent;
import mt.common.service.DataLockService;
import mt.common.service.IdGenerateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @Author Martin
 * @Date 2022/8/13
 */
public class IdGeneratorConfiguration {
	@Autowired
	private DataLockService dataLockService;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Bean
	@ConditionalOnMissingBean(IdGenerateService.class)
	public IdGenerateService idGenerateService() {
		SystemEntity.register(IdGenerate.class);
		return new IdGenerateService();
	}
	
	@EventListener
	public void afterCreateTable(AfterInitEvent afterInitEvent) {
		dataLockService.initLock("idGenerate", jdbcTemplate);
	}
}
