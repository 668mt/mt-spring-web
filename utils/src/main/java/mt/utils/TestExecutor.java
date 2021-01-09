package mt.utils;

/**
 * @Author Martin
 * @Date 2021/1/7
 */
public class TestExecutor {
	
	public static void main(String[] args) {
		MtExecutor<Integer> mtExecutor = new MtExecutor<Integer>(2) {
			@Override
			public void doJob(Integer task) {
				System.out.println(Thread.currentThread().getName() + "-执行任务" + task + "开始");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println(Thread.currentThread().getName() + "-执行任务" + task + "结束");
			}
		};
		mtExecutor.setEvent(new MtExecutor.Event<Integer>() {
			@Override
			public void onTaskFinished(MtExecutor<Integer> mtExecutor) {
				mtExecutor.shutdown();
			}
		});
		for (int i = 0; i < 10; i++) {
			mtExecutor.submit(i);
		}
	}
	
}
