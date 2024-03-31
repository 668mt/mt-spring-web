package mt.test;

import lombok.Data;

import javax.persistence.Table;

/**
 * @Author Martin
 * @Date 2024/3/31
 */
@Data
@Table(name = "test")
public class TestEntity {
	private String name;
	private String password;
}
