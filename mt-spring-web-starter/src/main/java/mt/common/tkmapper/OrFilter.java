package mt.common.tkmapper;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Martin
 * @date 2020/4/30
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class OrFilter extends Filter {
	private Filter[] filters;
	
	public OrFilter(Filter...filters) {
		super();
		this.filters = filters;
	}
}
