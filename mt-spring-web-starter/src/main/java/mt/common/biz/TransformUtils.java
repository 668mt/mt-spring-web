package mt.common.biz;

import com.github.pagehelper.PageInfo;
import mt.common.utils.BeanUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @Author Martin
 * @Date 2024/4/28
 */
public class TransformUtils {
	public interface TransformHandler<Src, Target> {
		List<Target> transformList(List<Src> srcList);
	}
	
	public static <Src, Target> PageInfo<Target> transformPageInfo(@NotNull PageInfo<Src> pageInfo, @NotNull Class<Target> targetClass) {
		return transformPageInfo(pageInfo, srcs -> BeanUtils.batchTransform(srcs, targetClass));
	}
	
	public static <Src, Target> PageInfo<Target> transformPageInfo(@NotNull PageInfo<Src> pageInfo, @NotNull TransformHandler<Src, Target> transformHandler) {
		PageInfo<Target> targetPageInfo = new PageInfo<>();
		targetPageInfo.setPageNum(pageInfo.getPageNum());
		targetPageInfo.setPageSize(pageInfo.getPageSize());
		targetPageInfo.setTotal(pageInfo.getTotal());
		targetPageInfo.setPages(pageInfo.getPages());
		targetPageInfo.setList(transformHandler.transformList(pageInfo.getList()));
		return targetPageInfo;
	}
}
