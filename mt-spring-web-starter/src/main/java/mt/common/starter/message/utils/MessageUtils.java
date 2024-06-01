package mt.common.starter.message.utils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mt.common.annotation.Filter;
import mt.common.config.CommonProperties;
import mt.common.converter.Converter;
import mt.common.starter.message.annotation.BatchMessage;
import mt.common.starter.message.annotation.BatchMultipleMessage;
import mt.common.starter.message.annotation.Message;
import mt.common.starter.message.exception.FieldNotFoundException;
import mt.common.starter.message.messagehandler.BatchMessageHandler;
import mt.common.starter.message.messagehandler.DefaultMessageHandler;
import mt.common.starter.message.messagehandler.MessageHandler;
import mt.common.starter.message.messagehandler.MultipleFieldValue;
import mt.common.utils.SpringUtils;
import mt.utils.JsonUtils;
import mt.utils.ReflectUtils;
import mt.utils.RegexUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

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
@SuppressWarnings({"rawtypes", "unchecked"})
public class MessageUtils {
	
	private Map<String, MessageHandler> messageHandlers;
	private final ThreadLocal<String> jsonThreadLocal = new ThreadLocal<>();
	private final CommonProperties commonProperties;
	
	public MessageUtils(CommonProperties commonProperties, Map<String, MessageHandler> messageHandlers) {
		this.messageHandlers = messageHandlers;
		this.commonProperties = commonProperties;
	}
	
	public void refreshMessageHandlers() {
		messageHandlers = SpringUtils.getBeansOfType(MessageHandler.class);
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
	
	private MessageHandler getMessageHandler(@NotNull Class<? extends MessageHandler> clazz) {
		Assert.notNull(clazz, "handlerClass不能为空");
		//使用类名查找
		for (Map.Entry<String, MessageHandler> entry : messageHandlers.entrySet()) {
			MessageHandler messageHandler = entry.getValue();
			if (clazz.equals(messageHandler.getClass())) {
				return messageHandler;
			}
		}
		throw new IllegalStateException("找不到messageHandler：" + clazz.getSimpleName());
	}
	
	private MessageHandler getMessageHandler(@NotNull Message message) {
		String handleBeanName = message.handlerBeanName();
		//使用Bean的名字进行查找
		if (StringUtils.isNotBlank(handleBeanName)) {
			return messageHandlers.get(handleBeanName);
		}
		Class<? extends MessageHandler> clazz = message.value().equals(DefaultMessageHandler.class) ? message.handlerClass() : message.value();
		Assert.notNull(clazz, "handleBeanName和handlerClass不能同时为空");
		return getMessageHandler(clazz);
	}
	
	public Object message(@Nullable Object object, @Nullable String... includeFields) {
		return messageWithGroup(object, null, includeFields);
	}
	
	/**
	 * 入口
	 *
	 * @param object        对象
	 * @param group         分组
	 * @param includeFields 包含的字段，为空则全部
	 * @return 处理后的对象
	 */
	public Object messageWithGroup(@Nullable Object object, @Nullable String[] group, @Nullable String... includeFields) {
		//初始化所有messageHandler
		for (Map.Entry<String, MessageHandler> messageHandlerEntry : messageHandlers.entrySet()) {
			messageHandlerEntry.getValue().init();
		}
		Set<String> groupList = null;
		if (group != null) {
			groupList = Arrays.stream(group).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
		}
		Map<BatchMessageKey, List<BatchHandleTarget>> batchHandleTargetMap = new HashMap<>();
		try {
			MessageRecursiveParams params = new MessageRecursiveParams(object);
			params.setGroup(groupList);
			params.setBatchHandleTarget(batchHandleTargetMap);
			Object value = messageRecursive(params, includeFields);
			for (Map.Entry<BatchMessageKey, List<BatchHandleTarget>> batchMessageKeyListEntry : batchHandleTargetMap.entrySet()) {
				dealBatchMessage(params, batchMessageKeyListEntry.getKey(), batchMessageKeyListEntry.getValue());
			}
			return value;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return object;
		} finally {
			jsonThreadLocal.remove();
		}
	}
	
	/**
	 * 处理一个对象
	 *
	 * @param params
	 * @param includeFields
	 * @return
	 */
	private Object messageRecursive(@NotNull MessageRecursiveParams params, @Nullable String... includeFields) {
		Object object = params.getTarget();
		Set<String> group = params.getGroup();
		Map<BatchMessageKey, List<BatchHandleTarget>> batchHandleTargetMap = params.getBatchHandleTarget();
		if (object == null) {
			return null;
		}
		if (object instanceof Collection collection) {
			for (Object o : collection) {
				messageRecursive(params.copy(o), includeFields);
			}
			return object;
		}
		if (object instanceof Object[] array) {
			for (Object o : array) {
				messageRecursive(params.copy(o), includeFields);
			}
			return object;
		}
		if (object instanceof Map map) {
			for (Object o : map.entrySet()) {
				messageRecursive(params.copy(((Map.Entry) o).getValue()), includeFields);
			}
			return object;
		}
		
		List<String> includeList = null;
		if (includeFields != null && includeFields.length > 0) {
			includeList = Arrays.asList(includeFields);
		}
		if (jsonThreadLocal.get() == null) {
			//拉出缓存，有可能被hibernate代理
			jsonThreadLocal.set(JsonUtils.toJson(object));
		}
		//查找实体类所有字段
		List<Field> fields = ReflectUtils.findAllFields(object.getClass());
		if (CollectionUtils.isEmpty(fields)) {
			return object;
		}
		Map<String, Field> fieldMap = new HashMap<>();
		for (Field field : fields) {
			fieldMap.put(field.getName(), field);
		}
		
		for (Field field : fields) {
			try {
				if (CollectionUtils.isNotEmpty(includeList) && !includeList.contains(field.getName())) {
					continue;
				}
				if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				//获取注解
				Message message = AnnotatedElementUtils.findMergedAnnotation(field, Message.class);
				BatchMessage batchMessage = AnnotatedElementUtils.findMergedAnnotation(field, BatchMessage.class);
				BatchMultipleMessage batchMultipleMessage = AnnotatedElementUtils.findMergedAnnotation(field, BatchMultipleMessage.class);
				if (message != null) {
					if (!inGroup(group, message)) {
						continue;
					}
					doWithMessage(message, field, object);
				} else if (batchMessage != null) {
					String column = batchMessage.column();
					Field srcField = fieldMap.get(column);
					Assert.notNull(srcField, "找不到字段" + column);
					srcField.setAccessible(true);
					Object value = srcField.get(object);
					BatchMessageKey batchMessageKey = new BatchMessageKey(batchMessage.handlerClass(), batchMessage.params());
					batchHandleTargetMap.computeIfAbsent(batchMessageKey, k -> new ArrayList<>()).add(new BatchHandleTarget(field, object, value));
				} else if (batchMultipleMessage != null) {
					String[] columns = batchMultipleMessage.columns();
					MultipleFieldValue multipleFieldValue = new MultipleFieldValue();
					Object[] values = new Object[columns.length];
					for (int i = 0; i < columns.length; i++) {
						Field srcField = fieldMap.get(columns[i]);
						Assert.notNull(srcField, "找不到字段" + columns[i]);
						srcField.setAccessible(true);
						values[i] = srcField.get(object);
					}
					multipleFieldValue.setValues(values);
					BatchMessageKey multipleBatchMessageKey = new BatchMessageKey(batchMultipleMessage.handlerClass(), batchMultipleMessage.params());
					BatchHandleTarget multipleBatchHandleTarget = new BatchHandleTarget(field, object, multipleFieldValue);
					batchHandleTargetMap.computeIfAbsent(multipleBatchMessageKey, k -> new ArrayList<>()).add(multipleBatchHandleTarget);
				} else {
					//内嵌集合
					if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					if (Collection.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType()) || field.getType().isArray()) {
						field.setAccessible(true);
						messageRecursive(params.copy(field.get(object)), includeFields);
					} else if (isContinueMessage(field)) {
						//处理其它类型
						field.setAccessible(true);
						messageRecursive(params.copy(field.get(object)), includeFields);
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
		field.setAccessible(true);
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
	
	@SneakyThrows
	public Object[] parseParams(@NotNull Field field, @NotNull String[] params, @NotNull Object object) {
		if (params.length == 0) {
			field.setAccessible(true);
			return new Object[]{field.get(object)};
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
	@SneakyThrows
	private Object parseParam(@NotNull Field field, @NotNull String param, @NotNull Object object) {
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
				log.error("replaceVariable error:{}", e.getMessage(), e);
			}
		}
		return param;
	}
	
	@SneakyThrows
	private void dealBatchMessage(MessageRecursiveParams messageRecursiveParams, BatchMessageKey batchMessageKey, List<BatchHandleTarget> batchHandleTargets) {
		Class<? extends BatchMessageHandler<?, ?>> handlerClass = batchMessageKey.getHandlerClass();
		String[] params = batchMessageKey.getParams();
		Set set = new HashSet<>();
		Collection collection = new ArrayList<>();
		for (BatchHandleTarget batchHandleTarget : batchHandleTargets) {
			Object target = batchHandleTarget.getTarget();
			collection.add(target);
			set.add(batchHandleTarget.getFromFieldValue());
		}
		MessageHandler messageHandler = getMessageHandler(handlerClass);
		Map map;
		if (messageHandler instanceof BatchMessageHandler batchMessageHandler) {
			map = batchMessageHandler.handle(collection, set, params);
		} else {
			throw new IllegalStateException("找不到messageHandler：" + handlerClass.getSimpleName());
		}
		if (map != null) {
			for (BatchHandleTarget batchHandleTarget : batchHandleTargets) {
				Object value = map.get(batchHandleTarget.getFromFieldValue());
				if (value == null) {
					continue;
				}
				Field field = batchHandleTarget.getField();
				field.setAccessible(true);
				field.set(batchHandleTarget.getTarget(), messageRecursive(messageRecursiveParams.copy(value)));
			}
		}
	}
}
