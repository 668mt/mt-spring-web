package mt.common.service;

import java.util.List;

public interface QueryHandler<T2> {
	List<T2> doQuery();
}