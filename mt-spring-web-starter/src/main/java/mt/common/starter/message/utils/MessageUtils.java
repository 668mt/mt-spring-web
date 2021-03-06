package mt.common.starter.message.utils;

import com.github.pagehelper.PageInfo;
import com.sun.corba.se.impl.orbutil.graph.Graph;
import lombok.extern.slf4j.Slf4j;
import mt.common.annotation.Filter;
import mt.common.config.CommonProperties;
import mt.common.converter.Converter;
import mt.common.starter.message.annotation.Message;
import mt.common.starter.message.exception.FieldNotFoundException;
import mt.common.starter.message.messagehandler.DefaultMessageHandler;
import mt.common.starter.message.messagehandler.MessageHandler;
import mt.utils.JsUtils;
import mt.utils.JsonUtils;
import mt.utils.ReflectUtils;
import mt.utils.RegexUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

import javax.script.ScriptException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: MessageUtils
 * @Description:
 * @Author Martin
 * @date 2017-11-30 上午10:13:47
 */
@Slf4j
public class MessageUtils {
	
	private Map<String, MessageHandler> messageHandlers;
	
	private CommonProperties commonProperties;
	
	public MessageUtils(CommonProperties commonProperties, Map<String, MessageHandler> messageHandlers) {
		this.messageHandlers = messageHandlers;
		this.commonProperties = commonProperties;
	}
	
	/**
	 * 是否继续处理这个字段
	 *
	 * @param field
	 * @return
	 */
	private boolean isContinueMessage(@NotNull Field field) {
		Class<?> type = field.getType();
		if (type.getPackage() == null) {
			return false;
		}
		if (type.getPackage().getName().matches(commonProperties.getMessager().getDealPackage() + ".+")) {
			return true;
		}
		return type.equals(Object.class);
	}
	
	private MessageHandler getMessageHandler(@NotNull Message message) {
		String handleBeanName = message.handlerBeanName();
		//使用Bean的名字进行查找
		if (StringUtils.isNotBlank(handleBeanName)) {
			return messageHandlers.get(handleBeanName);
		}
		Class<? extends MessageHandler> clazz = message.value().equals(DefaultMessageHandler.class) ? message.handlerClass() : message.value();
		Assert.notNull(clazz, "handleBeanName和handlerClass不能同时为空");
		//使用类名查找
		for (Map.Entry<String, MessageHandler> entry : messageHandlers.entrySet()) {
			MessageHandler messageHandler = entry.getValue();
			if (clazz.equals(messageHandler.getClass())) {
				return messageHandler;
			}
		}
		throw new IllegalStateException("找不到messageHandler：" + clazz.getSimpleName());
	}
	
	public Object message(@Nullable Object object, @Nullable String... includeFields) {
		return messageWithGroup(object, null, includeFields);
	}
	
	public Object messageWithGroup(@Nullable Object object, @Nullable String[] group, @Nullable String... includeFields) {
		//初始化所有messageHandler
		for (Map.Entry<String, MessageHandler> messageHandlerEntry : messageHandlers.entrySet()) {
			messageHandlerEntry.getValue().init();
		}
		Set<String> groupList = null;
		if (group != null) {
			groupList = Arrays.stream(group).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
		}
		return messageRecursive(object, groupList, includeFields);
	}
	
	/**
	 * 处理一个对象
	 *
	 * @param object
	 * @param includeFields
	 * @return
	 */
	@SuppressWarnings({"unchecked"})
	public Object messageRecursive(@Nullable Object object, Set<String> group, @Nullable String... includeFields) {
		if (object == null) {
			return null;
		}
		if (object instanceof Collection) {
			return dealWithCollection((Collection) object, group, includeFields);
		}
		if (object instanceof PageInfo) {
			return dealWithPageInfo((PageInfo) object, group, includeFields);
		}
		if (object instanceof Map) {
			Map map = (Map) object;
			return dealWithMap(map, group, includeFields);
		}
		
		List<String> includeList = null;
		if (ArrayUtils.isNotEmpty(includeFields)) {
			includeList = Arrays.asList(includeFields);
		}
		//拉出mybatis缓存
		JsonUtils.toJson(object);
		//查找实体类所有字段
		List<Field> fields = ReflectUtils.findAllFields(object.getClass());
		if (CollectionUtils.isEmpty(fields)) {
			return object;
		}
		
		for (Field field : fields) {
			try {
				if (CollectionUtils.isNotEmpty(includeList) && !includeList.contains(field.getName())) {
					continue;
				}
				if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				field.setAccessible(true);
				//获取注解
				Message message = AnnotatedElementUtils.findMergedAnnotation(field, Message.class);
				if (message != null) {
					if (!inGroup(group, message)) {
						continue;
					}
					doWithMessage(message, field, object);
				} else {
					//内嵌集合
					if (Collection.class.isAssignableFrom(field.getType())) {
						Object value = field.get(object);
						if (value == null) {
							continue;
						}
						Collection collection = (Collection) value;
						for (Object o : collection) {
							messageRecursive(o, group);
						}
					}
					//处理其它类型
					if (isContinueMessage(field) && !Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
						if (field.get(object) == null) {
							continue;
						}
						messageRecursive(field.get(object), group);
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return object;
	}
	
	private boolean inGroup(Set<String> group, Message message) {
		if (CollectionUtils.isEmpty(group)) {
			return true;
		}
		Set<String> currentGroup = Arrays.stream(message.group()).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
		if (CollectionUtils.isEmpty(currentGroup)) {
			return true;
		}
		for (String s : group) {
			if (currentGroup.contains(s)) {
				return true;
			}
		}
		return false;
	}
	
	private void doWithMessage(Message message, Field field, Object object) throws Exception {
//		//条件
//		String condition = message.condition();
//		if (StringUtils.isNotBlank(condition)) {
//			//替换变量
//			condition = replaceVariable(condition, object);
//			//解析js条件表达式
//			Boolean result = JsUtils.eval(condition, Boolean.class);
//			//不满足条件
//			if (result == null || !result) {
//				return;
//			}
//		}
		//替换变量值
		Object[] params = parseParams(field, message.params(), object);
		//计算出结果
		MessageHandler messageHandler = getMessageHandler(message);
		Object value = messageHandler.handle(object, params, message.mark());
		if (value != null) {
			if (field.getType().isAssignableFrom(value.getClass())) {
				field.set(object, value);
			} else {
				log.warn("字段{}转码失败,fieldType:{}，valueClass:{}", field.getName(), field.getType(), value.getClass());
			}
		}
	}
	
	public Object[] parseParams(@NotNull Field field, @NotNull String[] params, @NotNull Object object) {
		if (params.length == 0) {
			field.setAccessible(true);
			try {
				return new Object[]{field.get(object)};
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		Object[] afterParams = new Object[params.length];
		for (int i = 0; i < params.length; i++) {
			afterParams[i] = parseParam(field, params[i], object);
		}
		return afterParams;
	}
	
	/**
	 * 解析参数值
	 *
	 * @param field  当前字段
	 * @param param  参数，为空则取当前字段，不为空 #id 表示取值id字段
	 * @param object 实体对象
	 * @return 解析后的值
	 */
	public Object parseParam(@NotNull Field field, @NotNull String param, @NotNull Object object) {
		try {
			if (StringUtils.isBlank(param)) {
				field.setAccessible(true);
				return field.get(object);
			}
			if (!param.contains("#")) {
				return param;
			}
			String fieldName = RegexUtils.findFirst(param, "#(\\w+)", 1);
			Field field1 = ReflectUtils.findField(object.getClass(), fieldName);
			if (field1 == null) {
				throw new FieldNotFoundException("找不到字段：" + fieldName);
			}
			field1.setAccessible(true);
			return field1.get(object);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String replaceVariable(String param, Object object) {
		return replaceVariable(param, object, true);
	}
	
	/**
	 * 替换变量
	 *
	 * @param param
	 * @param object
	 * @return
	 */
	public static String replaceVariable(String param, Object object, boolean checkSqlInject) {
		if (object == null) {
			return param;
		}
		if (StringUtils.isBlank(param)) {
			return param;
		}
		List<String> findList = RegexUtils.findList(param, "#(\\w+)", 1);
		if (CollectionUtils.isEmpty(findList)) {
			//没有变量设置
			return param;
		}
		for (String fieldName : findList) {
			
			Field findField = ReflectUtils.findField(object.getClass(), fieldName);
			if (findField == null) {
				continue;
			}
			try {
				findField.setAccessible(true);
				//获取字段值
				Object objectParam = findField.get(object);
				if (objectParam == null) {
					continue;
				}
				Filter annotation = AnnotatedElementUtils.getMergedAnnotation(findField, Filter.class);
				if (annotation != null) {
					Class<? extends Converter<?>> converter = annotation.converter();
					objectParam = converter.getDeclaredMethod("convert", Object.class).invoke(converter.newInstance(), objectParam);
				}
				String value = objectParam + "";
				if (checkSqlInject) {
					value = StringEscapeUtils.escapeSql(value);
				}
				//进行替换
				param = param.replace("#" + fieldName, value);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return param;
	}
	
	/**
	 * message注解处理pageBean模型
	 *
	 * @param <T>
	 * @param pageInfo
	 * @return
	 */
	public <T> PageInfo<T> dealWithPageInfo(PageInfo<T> pageInfo, Set<String> group, String... includeFields) {
		if (pageInfo != null && CollectionUtils.isNotEmpty(pageInfo.getList())) {
			List<T> list = pageInfo.getList();
			for (T t : list) {
				messageRecursive(t, group, includeFields);
			}
		}
		if (pageInfo != null && CollectionUtils.isNotEmpty(pageInfo.getList())) {
			List<T> list = pageInfo.getList();
			for (T t : list) {
				messageRecursive(t, group, includeFields);
			}
		}
		return pageInfo;
	}
	
	public <T> Collection<T> dealWithCollection(Collection<T> list, Set<String> group, String... includeFields) {
		if (CollectionUtils.isNotEmpty(list)) {
			for (T t : list) {
				messageRecursive(t, group, includeFields);
			}
		}
		return list;
	}
	
	public Map dealWithMap(@NotNull Map map, Set<String> group, String[] includeFields) {
		for (Object entry : map.entrySet()) {
			messageRecursive(((Map.Entry) entry).getValue(), group, includeFields);
		}
		return map;
	}
	
}
