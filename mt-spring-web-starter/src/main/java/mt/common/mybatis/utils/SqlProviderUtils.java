package mt.common.mybatis.utils;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * @Author Martin
 * @Date 2024/7/25
 */
public class SqlProviderUtils {
	public static String exampleWhereClause(@Nullable String exampleName) {
		String prefix = StringUtils.isNotBlank(exampleName) ? exampleName + "." : "";
		return "<if test=\"_parameter != null\">" +
			"<where>\n" +
			" ${@tk.mybatis.mapper.util.OGNL@andNotLogicDelete(_parameter)}" +
			" <trim prefix=\"(\" prefixOverrides=\"and |or \" suffix=\")\">\n" +
			"  <foreach collection=\"" + prefix + "oredCriteria\" item=\"criteria\">\n" +
			"    <if test=\"criteria.valid\">\n" +
			"      ${@tk.mybatis.mapper.util.OGNL@andOr(criteria)}" +
			"      <trim prefix=\"(\" prefixOverrides=\"and |or \" suffix=\")\">\n" +
			"        <foreach collection=\"criteria.criteria\" item=\"criterion\">\n" +
			"          <choose>\n" +
			"            <when test=\"criterion.noValue\">\n" +
			"              ${@tk.mybatis.mapper.util.OGNL@andOr(criterion)} ${criterion.condition}\n" +
			"            </when>\n" +
			"            <when test=\"criterion.singleValue\">\n" +
			"              ${@tk.mybatis.mapper.util.OGNL@andOr(criterion)} ${criterion.condition} #{criterion.value}\n" +
			"            </when>\n" +
			"            <when test=\"criterion.betweenValue\">\n" +
			"              ${@tk.mybatis.mapper.util.OGNL@andOr(criterion)} ${criterion.condition} #{criterion.value} and #{criterion.secondValue}\n" +
			"            </when>\n" +
			"            <when test=\"criterion.listValue\">\n" +
			"              ${@tk.mybatis.mapper.util.OGNL@andOr(criterion)} ${criterion.condition}\n" +
			"              <foreach close=\")\" collection=\"criterion.value\" item=\"listItem\" open=\"(\" separator=\",\">\n" +
			"                #{listItem}\n" +
			"              </foreach>\n" +
			"            </when>\n" +
			"          </choose>\n" +
			"        </foreach>\n" +
			"      </trim>\n" +
			"    </if>\n" +
			"  </foreach>\n" +
			" </trim>\n" +
			"</where>" +
			"</if>";
	}
}
