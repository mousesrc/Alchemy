package index.alchemy.core;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import index.alchemy.core.debug.AlchemyRuntimeException;
import index.project.version.annotation.Omega;

@Omega
@ThreadSafe
public final class AlchemyThreadManager {
	
	private static final Logger logger = LogManager.getLogger(AlchemyThreadManager.class.getSimpleName());
	
	public static class OtherThreadThrowable extends Throwable {
		
		public final Thread thread = Thread.currentThread();

		public OtherThreadThrowable(Throwable e) {
			super(e);
		}
		
	}
	
	public AlchemyThreadManager() {
		this(1, 1, 10, 50);
	}
	
	public AlchemyThreadManager(int max, int skip) {
		this(1, max, 10, skip);
	}

	public AlchemyThreadManager(int max, int listAddThreshold, int skip) {
		this(1, max, listAddThreshold, skip);
	}

	public AlchemyThreadManager(int min, int max, int listAddThreshold, int skipFlag) {
		this.skipFlag = skipFlag;
		this.listAddThreshold = listAddThreshold;
		this.max = Math.max(Math.min(max, Runtime.getRuntime()
				.availableProcessors() - 1), 1);
		this.min = Math.max(Math.min(max, min), 1);
		for (int i = 0; i < this.min; i++)
			addThread();
	}
	
	private static int id;
	private int index = -1, size = -1, min, max, listAddThreshold, warning, num, skipFlag;
	private List<Threads> lt = Lists.newArrayList();
	private WriteLock lock = new ReentrantReadWriteLock().writeLock();
	
	private static int nextId() { return id++; }

	private final class Threads extends Thread {
		
		{
			setName("AlchemyThreadManager-" + nextId());
			start();
		}

		private int skip;
		private boolean running = true;
		private List<Runnable> list = Lists.newLinkedList();

		@Override
		public void run() {
			while (running) {
				if (list.size() > listAddThreshold)
					if (list.size() > skipFlag)
						list.clear();
					else
						addThread();
				if (list.size() > 0) {
					lock.lock();
					Runnable run = list.get(0);
					lock.unlock();
					try {
						run.run();
					} catch (Throwable e) {
						AlchemyModLoader.logger.error("[ThreadManager]Catch a Throwable in runtime loop: ");
						AlchemyRuntimeException.onException(new OtherThreadThrowable(e));
					}
					lock.lock();
					list.remove(0);
					lock.unlock();
					skip = 0;
				} else
					try {
						if (++skip > 100) {
							if (size > 1) {
								break;
							} else
								Thread.sleep(1000);
						} else
							Thread.sleep(10);
					} catch (Exception e) {}
			}
			deleltThread(Threads.this);
		}
	}

	private void deleltThread(Threads t) {
		lock.lock();
		lt.remove(t);
		size--;
		lock.unlock();
	}

	public void addThread() {
		if (size > max && ++warning > 100) {
			logger.error("Warning: ThreadManager can't meet the list needs.(" + ++num + ")");
			return;
		}
		lock.lock();
		lt.add(new Threads());
		size++;
		lock.unlock();
	}

	public void add(Runnable r) {
		lock.lock();
		lt.get(++index > size ? 0 : index).list.add(r);
		lock.unlock();
	}
	
	public static Thread runOnNewThread(Runnable runnable) {
		return new Thread(runnable) {
			
			{
				setName("Alchemy-" + nextId());
				start();
			}
			 
		};
	}
	
}
