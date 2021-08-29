package mt.common.paramcheck;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public interface ParameterChecker<AnnotationType, ParameterType> {
	boolean isValid(ParameterType value);
	
	String errorMsg(AnnotationType annotation, Method method, Parameter parameter, ParameterType value, Object target);
}