package mt.spring.rocketmq.utils;

/**
 * @Author Martin
 * @Date 2024/6/1
 */
public class TopicNameUtils {
	public static String getSafetyTopicName(String topicName) {
		return topicName.replaceAll("[:.]", "-");
	}
}
