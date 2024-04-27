package mt.common.entity.po;

import lombok.Data;
import mt.common.annotation.CreatedByUserName;
import mt.common.annotation.CreatedDate;
import mt.common.annotation.UpdatedByUserName;
import mt.common.annotation.UpdatedDate;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import java.util.Date;

/**
 * @Author Martin
 * @Date 2024/4/18
 */
@Data
public class BaseEntity {
	@Id
	@KeySql(useGeneratedKeys = true)
	@Column(updatable = false)
	private Long id;
	@CreatedByUserName
	private String createdBy;
	@UpdatedByUserName
	private String updatedBy;
	@CreatedDate
	private Date createdDate;
	@UpdatedDate
	private Date updatedDate;
}
