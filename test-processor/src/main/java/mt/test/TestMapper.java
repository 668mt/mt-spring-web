package mt.test;

import org.mapstruct.Mapper;

/**
 * @Author Martin
 * @Date 2024/3/31
 */
@Mapper
public interface TestMapper {
	TestEntity toTestEntity(TestEntityDTO testEntityDTO);
}
