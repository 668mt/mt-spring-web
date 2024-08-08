package mt.common.config.log;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.build.ToStringPlugin;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
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
		if (bean instanceof Executor) {
			try {
				Enhancer enhancer = new Enhancer();
				enhancer.setSuperclass(bean.getClass());
				enhancer.setCallback((MethodInterceptor) (o, method, objects, methodProxy) -> {
					if (method.getName().equals("execute")) {
						return method.invoke(bean, new TraceRunnable((Runnable) objects[0]));
					} else {
						return method.invoke(bean, objects);
					}
				});
				return enhancer.create();
			} catch (Exception e) {
				log.warn("trace代理创建失败：{}", e.getMessage());
			}
		}
		return bean;
	}
}
