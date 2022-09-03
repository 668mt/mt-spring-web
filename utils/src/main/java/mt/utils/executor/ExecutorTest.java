package mt.utils.executor;

/**
 * @Author Martin
 * @Date 2021/11/1
 */
public class ExecutorTest {
	public static void main(String[] args) {
		MtExecutor<String> mtExecutor = new MtExecutor<String>() {
			@Override
			public void doJob(String task) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		for (int i = 0; i < 100; i++) {
			mtExecutor.submit("" + i);
		}
	
	}
}
