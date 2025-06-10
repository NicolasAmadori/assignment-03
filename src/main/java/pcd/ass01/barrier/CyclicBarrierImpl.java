package pcd.ass01.barrier;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CyclicBarrierImpl implements Barrier {

    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final int nTotal;
    private int nArrived;
    private int currentGeneration;

    public CyclicBarrierImpl(int nTotal) {
        this.nTotal = nTotal;
        this.nArrived = 0;
        this.currentGeneration = 0;
    }

    @Override
    public void hitAndWaitAll() {
        try {
            lock.lock();
            int generation = currentGeneration;
            nArrived++;
            if (nArrived == nTotal) {
                nArrived = 0;
                currentGeneration++;
                condition.signalAll();
            } else {
                while (nArrived < nTotal && currentGeneration == generation) {
                    condition.await();
                }
            }
        } catch (InterruptedException e) {
            //throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}