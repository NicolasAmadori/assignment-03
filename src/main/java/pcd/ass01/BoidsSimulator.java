package pcd.ass01;

import pcd.ass01.barrier.CyclicBarrierImpl;
import pcd.ass01.monitor.BooleanMonitor;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BoidsSimulator {

    private BoidsModel model;
    private Optional<BoidsView> view;
    private static final int FRAMERATE = 25;
    private int framerate;

    private final BooleanMonitor startStopmonitor;
    private final BooleanMonitor pauseResumeMonitor;

    public BoidsSimulator(BoidsModel model) {
        this.model = model;
        view = Optional.empty();
        this.startStopmonitor = new BooleanMonitor(false);
        this.pauseResumeMonitor = new BooleanMonitor(true);
    }

    public void attachView(BoidsView view) {
        this.view = Optional.of(view);
    }

    public void resumeSimulator() {
        pauseResumeMonitor.set(true);
    }

    public void pauseSimulator() {
        pauseResumeMonitor.set(false);
    }

    public void startSimulator(int nBoids) {
        model.setBoids(nBoids);
        startStopmonitor.set(true);
    }

    public void stopSimulator() {
        startStopmonitor.set(false);
        pauseResumeMonitor.set(true);
    }

    public void runSimulationLoop() {
        while (true) {
            startStopmonitor.waitForCondition(true);
            runSimulation();
        }
    }

    private void runSimulation() {
        var boids = model.getBoids();
        var barrierVel = new CyclicBarrierImpl(boids.size());
        var barrierSync = new CyclicBarrierImpl(boids.size() + 1);

        var threads = boids.stream().map(b -> Thread.ofVirtual().start(() -> {
            while(true) {
                barrierSync.hitAndWaitAll();
                b.updateVelocity(model);
                barrierVel.hitAndWaitAll();
                b.updatePos(model);
                barrierSync.hitAndWaitAll();
            }
        })).toList();

        while (startStopmonitor.get()) {
            pauseResumeMonitor.waitForCondition(true);
            if(!startStopmonitor.get()) {
                break;
            }

            var t0 = System.currentTimeMillis();
            barrierSync.hitAndWaitAll();//Last, breaking barrier
            barrierSync.hitAndWaitAll();//First, wait

            if (view.isPresent()) {
                view.get().update(framerate);
                var t1 = System.currentTimeMillis();
                var dtElapsed = t1 - t0;
                var frameratePeriod = 1000/FRAMERATE;

                if (dtElapsed < frameratePeriod) {
                    try {
                        Thread.sleep(frameratePeriod - dtElapsed);
                    } catch (Exception ex) {}
                    framerate = FRAMERATE;
                } else {
                    framerate = (int) (1000/dtElapsed);
                }
            }

        }
        threads.forEach(Thread::interrupt);
    }
}
