package mt.common.config;

import mt.utils.BasePackageUtils;
import mt.utils.common.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author Martin
 * @Date 2019/8/27
 */
public class CommonEnvironmentPostProcessor implements EnvironmentPostProcessor {
	private final AtomicBoolean inited = new AtomicBoolean(false);
	
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (inited.get()) {
			return;
		}
		MutablePropertySources propertySources = environment.getPropertySources();
		Map<String, Object> params = new HashMap<>();
		
		Class<?> mainApplicationClass = application.getMainApplicationClass();
		if (mainApplicationClass == null) {
			return;
		}
		String basePackage = ObjectUtils.nullAsDefault(environment.getProperty("project.base-package", String.class), environment.getProperty("project.basePackage", String.class), BasePackageUtils.getBasePackage(mainApplicationClass));
		if (StringUtils.isNotBlank(basePackage)) {
			params.put("project.base-package", basePackage);
			params.put("project.basePackage", basePackage);
		}
		propertySources.addFirst(new CommonPropertySource("commonPropertyResource", params));
		inited.set(true);
	}
}
