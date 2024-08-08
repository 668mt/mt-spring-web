package mt.common.config.log;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * @Author Martin
 * @Date 2023/9/15
 */
@Component
@Slf4j
public class TraceExecutorBeanFactory implements BeanPostProcessor {
	@Override
	public Object postProcessBeforeInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
		if (bean instanceof ExecutorService) {
			try {
				return new TraceExecutor((ExecutorService) bean);
			} catch (Exception e) {
				log.warn("trace代理创建失败：{}", e.getMessage());
			}
		}
		return bean;
	}
}
