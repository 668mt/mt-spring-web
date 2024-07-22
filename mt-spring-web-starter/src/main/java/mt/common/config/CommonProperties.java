package mt.common.config;

import lombok.Data;
import org.bouncycastle.asn1.x509.GeneralName;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author Martin
 * @Date 2018-9-7
 */
@ConfigurationProperties(prefix = "mt.config")
@Data
@Component
public class CommonProperties {
	
	private String basePackage;
	private String[] daoPackage;
	
	@NestedConfigurationProperty
	private GeneratorConfig generator = new GeneratorConfig();
	/**
	 * 主键管理的表名
	 */
	private String idGenerateTableName = "idGenerate";
	/**
	 * 数据锁的表名
	 */
	private String dataLockTableName = "dataLock";
	private String loggingDaoPackageLevel = "DEBUG";
	private String loggingRedisLevel = "ERROR";
	
	/**
	 * 信息处理
	 */
	@NestedConfigurationProperty
	private Messager messager = new Messager();
	
	@Data
	public static class GeneratorConfig {
		private Boolean enabled;
		private List<String> packages;
	}
	
	@Data
	public static class Messager {
		private String dealPackage = "mt";
		/**
		 * 自动开启信息处理
		 */
		private Boolean autoMessage = true;
	}
	
	/**
	 * 断言拦截的包名
	 */
	private String assertPackageName;
}
