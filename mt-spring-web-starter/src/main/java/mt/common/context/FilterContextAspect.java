package mt.common.context;

import jakarta.servlet.http.HttpServletRequest;
import mt.common.context.annotation.IgnoreFilterContext;
import mt.common.context.annotation.UseFilterContext;
import mt.utils.common.Assert;
import mt.utils.common.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2024/4/27
 */
@Aspect
@Component
public class FilterContextAspect implements InitializingBean {
	@Autowired(required = false)
	private List<FilterContext> filterContexts;
	private final Map<String, FilterContext> filterContextMap = new HashMap<>(16);
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if (CollectionUtils.isNotEmpty(filterContexts)) {
			for (FilterContext filterContext : filterContexts) {
				filterContextMap.put(filterContext.name(), filterContext);
			}
		}
	}
	
	public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		Signature signature = proceedingJoinPoint.getSignature();
		MethodSignature methodSignature = (MethodSignature) signature;
		Method targetMethod = methodSignature.getMethod();
		Class<?> targetClass = proceedingJoinPoint.getTarget().getClass();
		UseFilterContext useFilterContext = AnnotatedElementUtils.getMergedAnnotation(targetClass, UseFilterContext.class);
		if (useFilterContext == null) {
			return proceedingJoinPoint.proceed();
		}
		IgnoreFilterContext ignoreFilterContext = AnnotatedElementUtils.getMergedAnnotation(targetMethod, IgnoreFilterContext.class);
		if (ignoreFilterContext != null) {
			return proceedingJoinPoint.proceed();
		}
		
		String[] contextNames = useFilterContext.contextNames();
		List<FilterContext> filterContexts = getFilterContexts(contextNames);
		try {
			ServletRequestAttributes attributes = getRequestContext();
			Assert.notNull(attributes, "ServletRequestAttributes not found");
			HttpServletRequest request = attributes.getRequest();
			FilterContextHolder.set(filterContexts);
			for (FilterContext filterContext : filterContexts) {
				filterContext.prepareContext(request);
			}
			return proceedingJoinPoint.proceed();
		} finally {
			for (FilterContext filterContext : filterContexts) {
				filterContext.clearContext();
			}
			FilterContextHolder.remove();
		}
	}
	
	private List<FilterContext> getFilterContexts(String[] contextNames) {
		return Arrays.stream(contextNames).map(s -> {
			FilterContext filterContext = filterContextMap.get(s);
			Assert.notNull(filterContext, "FilterContext not found:" + s);
			return filterContext;
		}).collect(Collectors.toList());
	}
	
	public ServletRequestAttributes getRequestContext() {
		return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
	}
	
	@Around("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
	public Object doRequestMappingAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		return doAround(proceedingJoinPoint);
	}
	
	@Around("@annotation(org.springframework.web.bind.annotation.GetMapping)")
	public Object doGetMappingAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		return doAround(proceedingJoinPoint);
	}
	
	@Around("@annotation(org.springframework.web.bind.annotation.PostMapping)")
	public Object doPostMappingAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		return doAround(proceedingJoinPoint);
	}
	
	@Around("@annotation(org.springframework.web.bind.annotation.PutMapping)")
	public Object doPutMappingAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		return doAround(proceedingJoinPoint);
	}
	
	@Around("@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
	public Object doDeleteMappingAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		return doAround(proceedingJoinPoint);
	}
	
	@Around("@annotation(org.springframework.web.bind.annotation.PatchMapping)")
	public Object doPatchMappingAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		return doAround(proceedingJoinPoint);
	}
}
