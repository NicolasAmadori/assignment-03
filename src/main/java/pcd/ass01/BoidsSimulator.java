package pcd.ass01;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import pcd.ass01.actors.BoidActor;
import static pcd.ass01.actors.BoidExchangeProtocol.*;
import static pcd.ass01.SimulatorExchangeProtocol.*;

import pcd.ass01.actors.BoidExchangeProtocol;
import pcd.ass01.monitor.BooleanMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BoidsSimulator extends AbstractActor {

    private BoidsModel model;
    private Optional<BoidsView> view;
    private static final int FRAMERATE = 25;
    private int framerate;
    private List<Boid> boids = new ArrayList<>();

    private List<ActorRef> boidsActors;

    /**
     * Behaviour to wait for a BootMsg to create the boid.
     * A stop message can be received to stop the actor
     */
    public Receive createReceive() {
        return receiveBuilder()
                .match(BootSimulationMsg.class, this::onBootMsg)
                .match(AttachViewMsg.class, this::onAttachView)
                .match(PauseSimulationMsg.class, this::onPauseSimulator)
                .match(StopSimulationMsg.class, this::onStopMsg)
                .match(ResumeSimulationMsg.class, this::onResumeSimulator)
                .match(StartSimulationMsg.class, this::onStartSimulator)
                .match(RunSimulationLoopMsg.class, this::onRunSimulationLoop)
                .match(RunSimulationMsg.class, this::onRunSimulation)
                .build();
    }

    public void onBootMsg(BootSimulationMsg msg) {
        this.model = msg.model();
        view = Optional.empty();
    }

    public void onAttachView(AttachViewMsg msg) {
        this.view = Optional.of(msg.view());
    }

    public void onResumeSimulator(ResumeSimulationMsg msg) {
        boidsActors.forEach(a -> a.tell(new ResumeMsg(), ActorRef.noSender()));
    }

    public void onPauseSimulator(PauseSimulationMsg msg) {
        boidsActors.forEach(a -> a.tell(new PauseMsg(), ActorRef.noSender()));
    }

    public void onStartSimulator(StartSimulationMsg msg) {
        boidsActors = new ArrayList<>();

        for (int i = 0; i < msg.nBoids(); i++) {
            var boid = this.getContext().actorOf(Props.create(BoidActor.class), "boid-" + i);
            boidsActors.add(boid);
        }

        for (var boidActor : boidsActors) {
            boidActor.tell(new BootMsg(boidsActors, model), ActorRef.noSender());
        }
    }

    public void onStopSimulator(StopSimulationMsg msg) {
        boidsActors.forEach(a -> a.tell(new BoidExchangeProtocol.StopMsg(), ActorRef.noSender()));
    }

    public void onRunSimulationLoop(RunSimulationLoopMsg msg) {
        while (true) {
            //startStopmonitor.waitForCondition(true);
            onRunSimulation(new RunSimulationMsg());
        }
    }

    private void onRunSimulation(RunSimulationMsg msg) {
        while (true) {
            var t0 = System.currentTimeMillis();

            boids = new ArrayList<>();
            for (var boidActor : boidsActors) {
                boidActor.tell(new UpdateMsg(this.getSelf()), this.getSelf());
            }
//            this.getContext().become();

            if (view.isPresent()) {
//                view.get().update(framerate);
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
    }


    private void onSendBoidMsg(SendBoidMsg msg) {
        log("SendBoidMsg received");
        boids.add(msg.boid());
        if (boids.size() == boidsActors.size()) {
            log("Received all the updated positions by the boid actors");
        }
    }

    private void onStopMsg(StopSimulationMsg msg) {
        this.getContext().stop(this.getSelf());
    }

    private void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ " + this.getSelf().path().name() + " ] " + msg);
    }
}
