package mt.common.config;

import lombok.extern.slf4j.Slf4j;
import mt.utils.BasePackageUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.AnnotatedElementUtils;
import tk.mybatis.mapper.util.Assert;
import tk.mybatis.spring.annotation.MapperScan;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author Martin
 * @Date 2018/9/17
 */
@Configuration
@ComponentScan(basePackages = {"mt.common", "mt.utils"})
@Slf4j
public class MtWebAutoConfiguration implements InitializingBean {
	
	@Autowired
	private LoggingSystem loggingSystem;
	@Autowired
	private ApplicationContext context;
	@Autowired
	private CommonProperties commonProperties;
	private final AtomicBoolean inited = new AtomicBoolean(false);
	
	@Override
	public void afterPropertiesSet() {
		if (inited.get()) {
			return;
		}
		Map<String, Object> beansWithAnnotation = context.getBeansWithAnnotation(SpringBootApplication.class);
		
		Assert.notEmpty(beansWithAnnotation, "SpringBootApplication 启动类不存在");
		for (Map.Entry<String, Object> stringObjectEntry : beansWithAnnotation.entrySet()) {
			Object value = stringObjectEntry.getValue();
			String basePackage = BasePackageUtils.getBasePackage(value.getClass());
			if (StringUtils.isBlank(commonProperties.getBasePackage())) {
				commonProperties.setBasePackage(basePackage);
			}
			if (ArrayUtils.isEmpty(commonProperties.getGenerateEntityPackages())) {
				commonProperties.setGenerateEntityPackages(new String[]{basePackage + ".entity"});
			}
			break;
		}
		
		Set<String> daoPackageList = new HashSet<>();
		daoPackageList.add("mt.common.dao");
		Map<String, Object> beansWithAnnotation1 = context.getBeansWithAnnotation(MapperScan.class);
		for (Map.Entry<String, Object> stringObjectEntry : beansWithAnnotation1.entrySet()) {
			Object value = stringObjectEntry.getValue();
			MapperScan mapperScan = AnnotatedElementUtils.findMergedAnnotation(value.getClass(), MapperScan.class);
			if (mapperScan != null) {
				String[] daoPackages1 = mapperScan.value();
				String[] daoPackages2 = mapperScan.basePackages();
				if (ArrayUtils.isNotEmpty(daoPackages1)) {
					daoPackageList.addAll(Arrays.asList(daoPackages1));
				} else {
					daoPackageList.addAll(Arrays.asList(daoPackages2));
				}
			}
		}
		
		Map<String, Object> beansWithAnnotation2 = context.getBeansWithAnnotation(tk.mybatis.spring.annotation.MapperScan.class);
		for (Map.Entry<String, Object> stringObjectEntry : beansWithAnnotation2.entrySet()) {
			Object value = stringObjectEntry.getValue();
			tk.mybatis.spring.annotation.MapperScan mapperScan = AnnotatedElementUtils.findMergedAnnotation(value.getClass(), tk.mybatis.spring.annotation.MapperScan.class);
			if (mapperScan != null) {
				String[] daoPackages1 = mapperScan.value();
				String[] daoPackages2 = mapperScan.basePackages();
				if (ArrayUtils.isNotEmpty(daoPackages1)) {
					daoPackageList.addAll(Arrays.asList(daoPackages1));
				} else {
					daoPackageList.addAll(Arrays.asList(daoPackages2));
				}
			}
		}
		
		if (ArrayUtils.isEmpty(commonProperties.getDaoPackage())) {
			commonProperties.setDaoPackage(daoPackageList.toArray(new String[0]));
		}
		
		log.info("当前服务basePackage:{}", commonProperties.getBasePackage());
		if (commonProperties.getGenerateEntityPackages() != null) {
			log.info("当前服务generateEntityPackages:{}", Arrays.toString(commonProperties.getGenerateEntityPackages()));
		}
		if (commonProperties.getDaoPackage() != null) {
			String[] daoPackage = commonProperties.getDaoPackage();
			log.info("当前服务daoPackage:{}", Arrays.toString(daoPackage));
			for (String s : daoPackage) {
				LoggerConfiguration loggerConfiguration = loggingSystem.getLoggerConfiguration(s);
				if (loggerConfiguration == null || loggerConfiguration.getConfiguredLevel() == null) {
					log.info("默认设置日志级别{}为{}", s, commonProperties.getLoggingDaoPackageLevel().toUpperCase());
					loggingSystem.setLogLevel(s, LogLevel.valueOf(commonProperties.getLoggingDaoPackageLevel().toUpperCase()));
				} else {
					log.info("daoPackage[{}]的日志级别是{}", s, loggerConfiguration.getEffectiveLevel());
				}
			}
		}
		//设置redis日志级别
		LoggerConfiguration redisConfig = loggingSystem.getLoggerConfiguration("io.lettuce.core.protocol");
		if (redisConfig == null || redisConfig.getConfiguredLevel() == null) {
			loggingSystem.setLogLevel("io.lettuce.core.protocol", LogLevel.valueOf(commonProperties.getLoggingRedisLevel().toUpperCase()));
		}
		inited.set(true);
	}
}
