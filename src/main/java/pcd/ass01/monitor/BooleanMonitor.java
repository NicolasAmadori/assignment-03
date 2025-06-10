package pcd.ass01.monitor;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BooleanMonitor {

    private boolean value;
    private final Lock lock;
    private final Condition cond;

    public BooleanMonitor(boolean startingValue) {
        this.value = startingValue;
        this.lock= new ReentrantLock();
        this.cond = lock.newCondition();
    }

    public void set(boolean val) {
        try {
            lock.lock();
            value = val;
            cond.signalAll();
        }
        finally {
            lock.unlock();
        }
    }

    public boolean get() {
        try {
            lock.lock();
            return value;
        }
        finally {
            lock.unlock();
        }
    }

    public void waitForCondition(boolean target) {
        try {
            lock.lock();
            while(value != target) {
                try {
                    cond.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}