package mt.common.entity;


import lombok.Data;
import mt.common.annotation.CreatedDate;
import mt.common.annotation.GenerateOrder;
import mt.common.annotation.UpdatedDate;
import mt.common.annotation.Version;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Martin
 * @ClassName: BaseFields
 * @Description:
 * @date 2018-3-30 下午5:57:57
 */
@Data
public class BaseFields implements Serializable {
	
	private static final long serialVersionUID = 4861455153422740537L;
	
	@CreatedDate
	@GenerateOrder(5)
	private Date createdDate;
	
	@Version
	@GenerateOrder(5)
	private Long version;
	
	@UpdatedDate
	@GenerateOrder(5)
	private Date lastModifiedDate;
}
