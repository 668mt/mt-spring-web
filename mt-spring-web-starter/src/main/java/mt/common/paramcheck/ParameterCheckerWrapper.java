package mt.common.paramcheck;

import lombok.Data;

@SuppressWarnings("rawtypes")
@Data
public class ParameterCheckerWrapper {
	private Class annotationType;
	private Class parameterType;
	private ParameterChecker parameterChecker;
}