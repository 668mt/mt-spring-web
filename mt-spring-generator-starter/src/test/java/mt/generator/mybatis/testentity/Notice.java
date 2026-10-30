package mt.generator.mybatis.testentity;

import lombok.Data;
import mt.common.annotation.ForeignKey;
import mt.common.entity.po.BaseEntity;
import mt.generator.mybatis.annotation.Index;
import mt.generator.mybatis.annotation.Indexs;

import javax.persistence.Column;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 测试用的实体，覆盖各种列类型、索引、外键
 *
 * @author Martin
 */
@Data
@Table(name = "notice")
@Indexs({
		@Index(columns = "noticeType"),
		@Index(name = "idx_notice_user", columns = {"userId", "status"}),
})
public class Notice extends BaseEntity {

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "longtext")
	private String content;

	@Column(nullable = false)
	private Boolean isTop;

	@Column(nullable = false)
	private Integer noticeType;

	private Long userId;

	private Date startDate;

	private BigDecimal amount;

	private byte[] attachment;

	private Status status;

	/**
	 * 状态
	 */
	public enum Status {
		NEW, DONE
	}

	/**
	 * 带外键的实体
	 */
	@Data
	@Table(name = "notice_detail")
	public static class NoticeDetail extends BaseEntity {

		@Column(name = "notice_id", nullable = false)
		@ForeignKey(tableEntity = Notice.class, casecadeType = ForeignKey.CascadeType.DELETE)
		private Long noticeId;

		private String remark;
	}
}
