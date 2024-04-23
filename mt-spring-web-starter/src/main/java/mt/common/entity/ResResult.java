package mt.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author Martin
 * @Date 2019/12/27
 */
@Data
@NoArgsConstructor
public class ResResult<T> {
	private Status status;
	private String message;
	private T result;
	private String code;
	
	@JsonIgnore
	public boolean isSuccess() {
		return status != null && status == Status.ok;
	}
	
	public enum Status {
		ok, error
	}
	
	public ResResult(Status status, String message) {
		this.status = status;
		this.message = message;
	}
	
	public ResResult(Status status, String code, String message) {
		this.code = code;
		this.status = status;
		this.message = message;
	}
	
	public ResResult(T result) {
		this.status = Status.ok;
		this.result = result;
	}
	
	public static <T> ResResult<T> success(T data) {
		return new ResResult<>(data);
	}
	
	public static <T> ResResult<T> success() {
		return new ResResult<>(Status.ok, null);
	}
	
	public static <T> ResResult<T> error(String message) {
		return new ResResult<>(Status.error, message);
	}
	
	public static <T> ResResult<T> error(String code, String message) {
		return new ResResult<>(Status.error, code, message);
	}
}
