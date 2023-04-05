package test.hits;

import mt.common.hits.LocalHitsRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author Martin
 * @Date 2023/4/5
 */
public class TestRecorder {
	public static void main(String[] args) throws ExecutionException, InterruptedException {
		AtomicLong atomicLong = new AtomicLong(0);
		AtomicLong atomicLong2 = new AtomicLong(0);
		LocalHitsRecorder<String, String> localHitsRecorder = new LocalHitsRecorder<>((s, hitsMap) -> {
			for (Map.Entry<String, Long> stringLongEntry : hitsMap.entrySet()) {
				atomicLong.getAndAdd(stringLongEntry.getValue());
			}
		});
		long start = System.currentTimeMillis();
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		List<Future<?>> list = new ArrayList<>();
		//100个线程，每个线程100次
		for (int i = 0; i < 100; i++) {
			list.add(executorService.submit(() -> {
				for (int j = 0; j < 1000; j++) {
					atomicLong2.getAndAdd(1);
					localHitsRecorder.recordHits("default", "1", 1);
					localHitsRecorder.hitsDown();
				}
			}));
		}
		for (Future<?> future : list) {
			future.get();
		}
		System.out.println(atomicLong.get());
		System.out.println(atomicLong2.get());
		System.out.println(System.currentTimeMillis() - start);
		executorService.shutdown();
	}
}
