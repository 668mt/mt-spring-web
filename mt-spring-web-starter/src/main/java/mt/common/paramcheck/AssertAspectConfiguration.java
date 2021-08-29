package mt.common.paramcheck;

import mt.common.config.CommonProperties;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.aspectj.AspectJExpressionPointcutAdvisor;
import org.springframework.aop.framework.adapter.MethodBeforeAdviceInterceptor;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mt.common.utils.ClassTypeUtils.findGenericInterface;

/**
 * @Author Martin
 * @Date 2021/8/29
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@ConditionalOnProperty(name = "project.assert-package-name")
@Configuration
public class AssertAspectConfiguration {
	private final static Map<Class<? extends Annotation>, List<ParameterCheckerWrapper>> checkers = new HashMap<>();
	
	static {
		//默认注册
		register(new ParameterCheckers.NotNullChecker1());
		register(new ParameterCheckers.NotNullChecker2());
		register(new ParameterCheckers.NotBlankChecker());
		register(new ParameterCheckers.NotEmptyChecker());
	}
	
	/**
	 * 注册
	 *
	 * @param parameterChecker 参数校验器
	 */
	public static synchronized void register(ParameterChecker<?, ?> parameterChecker) {
		ParameterizedType genericInterface = findGenericInterface(parameterChecker.getClass(), ParameterChecker.class);
		if (genericInterface == null) {
			throw new IllegalArgumentException("注册失败，找不到ParameterChecker接口");
		}
		Type[] actualTypeArguments = genericInterface.getActualTypeArguments();
		Class<? extends Annotation> annotation = (Class<? extends Annotation>) actualTypeArguments[0];
		Class<?> parameterType = (Class<?>) actualTypeArguments[1];
		
		List<ParameterCheckerWrapper> parameterCheckers = checkers.get(annotation);
		if (parameterCheckers == null) {
			parameterCheckers = new ArrayList<>();
		}
		ParameterCheckerWrapper wrapper = new ParameterCheckerWrapper();
		wrapper.setAnnotationType(annotation);
		wrapper.setParameterType(parameterType);
		wrapper.setParameterChecker(parameterChecker);
		parameterCheckers.add(wrapper);
		checkers.put(annotation, parameterCheckers);
	}
	
	@Bean
	public MethodBeforeAdviceInterceptor interceptor() {
		return new MethodBeforeAdviceInterceptor(new AssertParameterBeforeAdvice());
	}

	@Bean
	public AspectJExpressionPointcutAdvisor aspectJExpressionPointcutAdvisor(CommonProperties commonProperties, MethodInterceptor interceptor) {
		String packageName = commonProperties.getAssertPackageName();
		AspectJExpressionPointcutAdvisor aspectJExpressionPointcutAdvisor = new AspectJExpressionPointcutAdvisor();
		aspectJExpressionPointcutAdvisor.setExpression("execution(* " + packageName + "..*(..))");
		aspectJExpressionPointcutAdvisor.setAdvice(interceptor);
		return aspectJExpressionPointcutAdvisor;
	}
//
//	@Bean
//	public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
//		return new DefaultAdvisorAutoProxyCreator();
//	}
	
	/**
	 * 拦截器
	 */
	public static class AssertParameterBeforeAdvice implements MethodBeforeAdvice {
		@Override
		public void before(Method method, @NotNull Object[] args, Object target) throws Throwable {
			Parameter[] parameters = method.getParameters();
			if (parameters == null) {
				return;
			}
			for (int i = 0; i < parameters.length; i++) {
				Parameter parameter = parameters[i];
				Object value = args[i];
				Annotation[] annotations = parameter.getAnnotations();
				if (annotations == null) {
					continue;
				}
				for (Annotation annotation : annotations) {
					Class<?> aClass = annotation.getClass();
					if (annotation instanceof Proxy) {
						aClass = aClass.getInterfaces()[0];
					}
					List<ParameterCheckerWrapper> parameterCheckers = checkers.get(aClass);
					if (parameterCheckers == null) {
						continue;
					}
					for (ParameterCheckerWrapper parameterCheckerWrapper : parameterCheckers) {
						ParameterChecker parameterChecker = parameterCheckerWrapper.getParameterChecker();
						Class parameterType = parameterCheckerWrapper.getParameterType();
						if (!parameterType.isAssignableFrom(parameter.getType())) {
							continue;
						}
						boolean valid = parameterChecker.isValid(value);
						if (!valid) {
							throw new IllegalArgumentException(parameterChecker.errorMsg(annotation, method, parameter, value, target));
						}
					}
				}
			}
		}
	}
}
