package mt.common.tkmapper;

/**
 * 运算符
 */
public enum Operator {
	
	/**
	 * 等于
	 */
	eq,
	
	/**
	 * 不等于
	 */
	ne,
	
	/**
	 * 大于
	 */
	gt,
	
	/**
	 * 小于
	 */
	lt,
	
	/**
	 * 大于等于
	 */
	ge,
	
	/**
	 * 小于等于
	 */
	le,
	
	/**
	 * 相似
	 */
	like,
	
	/**
	 * 包含
	 */
	in,
	
	/**
	 * 为Null
	 */
	isNull,
	
	/**
	 * 不为Null
	 */
	isNotNull,
	/**
	 * 等于，如果为null，则判断is null
	 */
	eqn,
	/**
	 * 不在集合里面
	 */
	notIn,
	/**
	 * 自定义条件
	 */
	condition,
	/**
	 * 在之间
	 */
	between,
	/**
	 * 不在之间
	 */
	notBetween,
	/**
	 * 不像
	 */
	notLike
}