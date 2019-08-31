package net.tiny.service;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PausableThreadPoolExecutor extends ThreadPoolExeutorWrapper implements PausableExecutorService {

    private boolean isPaused;
    private ReentrantLock pauseLock = new ReentrantLock();
    private Condition unpaused = pauseLock.newCondition();

    protected void beforeExecute(Thread t, Runnable r) {
        pauseLock.lock();
        try {
            while (isPaused)
                unpaused.await();
        } catch (InterruptedException ie) {
            t.interrupt();
        } finally {
            pauseLock.unlock();
        }
    }

    @Override
    public void pause() {
        pauseLock.lock();
        try {
            isPaused = true;
        } finally {
            pauseLock.unlock();
        }
    }

    @Override
    public void resume() {
        pauseLock.lock();
        try {
            isPaused = false;
            unpaused.signalAll();
        } finally {
            pauseLock.unlock();
        }
    }

    public int getActiveCount() {
    	return getDelgate().getActiveCount();
    }

    public int getCorePoolSize() {
    	return getDelgate().getCorePoolSize();
    }

    public int getLargestPoolSize() {
    	return getDelgate().getLargestPoolSize();
    }

    public int getMaximumPoolSize() {
    	return getDelgate().getMaximumPoolSize();
    }

    public int getPoolSize() {
    	return getDelgate().getPoolSize();
    }
}
