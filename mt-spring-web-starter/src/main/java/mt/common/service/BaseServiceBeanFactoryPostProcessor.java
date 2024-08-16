package mt.common.service;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import mt.common.annotation.Datasource;
import mt.common.mybatis.mapper.BaseMapper;
import mt.utils.common.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.mapperhelper.MapperHelper;
import tk.mybatis.spring.mapper.MapperFactoryBean;
import tk.mybatis.spring.mapper.SpringBootBindUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author Martin
 * @Date 2021/8/28
 * implements BeanFactoryPostProcessor, EnvironmentAware
 */
@Component
public class BaseServiceBeanFactoryPostProcessor implements InitializingBean {
	protected final Log logger = LogFactory.getLog(getClass());
	private MapperHelper mapperHelper;
	@Autowired
	private ConfigurableListableBeanFactory beanFactory;
	@Autowired
	private Environment environment;
	@Autowired(required = false)
	private final List<BaseRepositoryImpl> services = new ArrayList<>();
	private final AtomicBoolean inited = new AtomicBoolean(false);
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if (inited.get()) {
			return;
		}
		setMapperProperties(environment);
		init();
		inited.set(true);
	}
	
	private void init() {
		if (CollectionUtils.isEmpty(services)) {
			return;
		}
		
		//转换为子类，因为父类没有添加beanDefintion对象的api
		DefaultListableBeanFactory defaultbf = (DefaultListableBeanFactory) beanFactory;
		
		for (BaseRepositoryImpl baseService : services) {
			Class entityClass = baseService.getEntityClass();
			String simpleName = entityClass.getSimpleName() + "AutoMapper";
			String mapperName = "mt.common.dao." + simpleName;
			try {
				ClassPool pool = ClassPool.getDefault();
				pool.appendClassPath(new ClassClassPath(BaseMapper.class));
				CtClass ctClass = pool.makeInterface(mapperName, pool.getCtClass(BaseMapper.class.getName()));
				ctClass.setGenericSignature("Ljava/lang/Object;L" + BaseMapper.class.getName().replace(".", "/") + "<L" + entityClass.getName().replace(".", "/") + ";>;");
				// 将类搜索路径插入到搜索路径之前
				pool.appendClassPath(new ClassClassPath(ctClass.toClass()));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				throw new RuntimeException(e);
			}
			
			GenericBeanDefinition definition = new GenericBeanDefinition();
			// the mapper interface is the original class of the bean
			// but, the actual class of the bean is MapperFactoryBean
			definition.getConstructorArgumentValues().addGenericArgumentValue(mapperName);
			definition.setBeanClass(MapperFactoryBean.class);
			//设置通用 Mapper
			//不做任何配置的时候使用默认方式
			if (this.mapperHelper == null) {
				this.mapperHelper = new MapperHelper();
			}
			definition.getPropertyValues().add("mapperHelper", this.mapperHelper);
			definition.getPropertyValues().add("addToConfig", true);
			Datasource datasource = AnnotatedElementUtils.findMergedAnnotation(baseService.getClass(), Datasource.class);
			if (datasource != null) {
				String sqlSessionFactoryRef = datasource.sqlSessionFactoryRef();
				String sqlSessionTemplateRef = datasource.sqlSessionTemplateRef();
				if (StringUtils.isNotBlank(sqlSessionFactoryRef)) {
					definition.getPropertyValues().add("sqlSessionFactory", new RuntimeBeanReference(sqlSessionFactoryRef));
				}
				if (StringUtils.isNotBlank(sqlSessionTemplateRef)) {
					definition.getPropertyValues().add("sqlSessionTemplate", new RuntimeBeanReference(sqlSessionTemplateRef));
				}
			}
			
			definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
			defaultbf.registerBeanDefinition(simpleName, definition);
		}
	}
	
	private void setMapperProperties(Environment environment) {
		Config config = SpringBootBindUtil.bind(environment, Config.class, Config.PREFIX);
		if (mapperHelper == null) {
			mapperHelper = new MapperHelper();
		}
		if (config != null) {
			mapperHelper.setConfig(config);
		}
	}
}
