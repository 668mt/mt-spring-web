package mt.generator.mybatis;

import lombok.extern.slf4j.Slf4j;
import mt.common.config.CommonProperties;
import mt.common.entity.DataLock;
import mt.common.mybatis.event.AfterInitEvent;
import mt.common.mybatis.event.BeforeInitEvent;
import mt.common.service.DataLockService;
import mt.generator.mybatis.utils.GenerateHelper;
import mt.generator.mybatis.utils.IParser;
import mt.utils.common.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import tk.mybatis.mapper.autoconfigure.MybatisProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据库和表格自动生成器
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Company: </p>
 *
 * @author Martin
 * @date 2017-10-23 下午10:10:04
 * implements InitializingBean, ApplicationListener<ApplicationEvent>
 */
@Slf4j
public class Generator {
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	private CommonProperties commonProperties;
	@Autowired
	private MybatisProperties mybatisProperties;
	@Autowired
	private DataSourceProperties dataSourceProperties;
	@Autowired
	private List<IParser> parsers;
	private final AtomicBoolean loaded = new AtomicBoolean(false);
	
	@EventListener
	public void listener(ContextRefreshedEvent contextRefreshedEvent) throws Exception {
		if (contextRefreshedEvent.getApplicationContext().getParent() == null) {
			return;
		}
		if (!Boolean.TRUE.equals(commonProperties.getGenerator().getEnabled())) {
			return;
		}
		if (loaded.get()) {
			return;
		} else {
			loaded.set(true);
		}
		String[] entityPackages;
		String jdbcUrl;
		String driverClass;
		String user;
		String password;
		jdbcUrl = dataSourceProperties.getUrl();
		driverClass = dataSourceProperties.getDriverClassName();
		user = dataSourceProperties.getUsername();
		password = dataSourceProperties.getPassword();
		List<String> packages = commonProperties.getGenerator().getPackages();
		if (CollectionUtils.isEmpty(packages)) {
			entityPackages = new String[]{mybatisProperties.getTypeAliasesPackage()};
		} else {
			entityPackages = packages.toArray(new String[0]);
		}
		
		applicationEventPublisher.publishEvent(new BeforeInitEvent(this));
		
		//初始化
		log.info("Generator初始化，创建表的包名：{}", Arrays.toString(entityPackages));
		//初始化完成事件
		AfterInitEvent afterInitEvent = new AfterInitEvent(this);
		
		try {
			boolean generated = false;
			for (IParser parser : parsers) {
				if (parser.support(driverClass)) {
					new GenerateHelper(jdbcUrl, driverClass, user, password, parser).init(entityPackages, afterInitEvent);
					generated = true;
					break;
				}
			}
			if (!generated) {
				log.warn("没有找到合适的表生成器：" + driverClass);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			System.exit(0);
		}
		
		//注册初始化完成事件
		applicationEventPublisher.publishEvent(afterInitEvent);
		
	}
	
}
