package mt.spring.core.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamePrefixThreadFactory implements ThreadFactory {
	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;
	
	public NamePrefixThreadFactory(String namePrefix) {
		SecurityManager s = System.getSecurityManager();
		group = (s != null) ? s.getThreadGroup() :
				Thread.currentThread().getThreadGroup();
		this.namePrefix = namePrefix + "-thread-";
	}
	
	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r,
				namePrefix + threadNumber.getAndIncrement(),
				0);
		if (t.isDaemon())
			t.setDaemon(false);
		if (t.getPriority() != Thread.NORM_PRIORITY)
			t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}
}