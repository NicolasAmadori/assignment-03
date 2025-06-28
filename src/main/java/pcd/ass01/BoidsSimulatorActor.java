package pcd.ass01;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;
import pcd.ass01.actors.BoidActor;
import static pcd.ass01.actors.BoidExchangeProtocol.*;
import static pcd.ass01.SimulatorExchangeProtocol.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BoidsSimulatorActor extends AbstractActorWithStash {

    private BoidsModel model;
    private Optional<BoidsView> view;
    private static final int FRAMERATE = 25;
    private int framerate;
    private List<Boid> boids = new ArrayList<>();
    private List<Boid> collectedBoids;
    private long t0;
    private List<ActorRef> boidsActors = new ArrayList<>();

    /**
     * Behaviour to wait for a BootMsg to create the boid.
     * A stop message can be received to stop the actor
     */
    public Receive createReceive() {
        return receiveBuilder()
                .match(BootSimulationMsg.class, this::onBootMsg)
                .match(AttachViewMsg.class, this::onAttachView)
                .match(StopSimulatorMsg.class, this::onStopMsg)
                .build();
    }

    public void onBootMsg(BootSimulationMsg msg) {
        this.model = msg.model();
        view = Optional.empty();
    }

    public void onAttachView(AttachViewMsg msg) {
        this.view = Optional.of(msg.view());
        this.getContext().become(receiverStart());
    }

    public Receive receiverStart() {
        return receiveBuilder()
                .match(StartSimulationMsg.class, this::onStartSimulator)
                .match(StopSimulatorMsg.class, this::onStopMsg)
                .build();
    }

    public void onStartSimulator(StartSimulationMsg msg) {
        for (int i = 0; i < msg.nBoids(); i++) {
            var boidActor = this.getContext().actorOf(Props.create(BoidActor.class).withDispatcher("my-blocking-dispatcher"), "B-" + i);
            boidsActors.add(boidActor);
            var boid = new Boid(model);
            boids.add(boid);
            boidActor.tell(new BootMsg(boidsActors, boid), this.getSelf());
        }
        this.getContext().become(receiverRun());
        this.getSelf().tell(new RunSimulationMsg(), this.getSelf());
    }

    private Receive receiverRun() {
        return receiveBuilder()
                .match(RunSimulationMsg.class, this::onRunSimulation)
                .match(PauseSimulationMsg.class, this::onPauseSimulator)
                .match(StopSimulationMsg.class, this::onStopSimulator)
                .build();
    }

    private void onRunSimulation(RunSimulationMsg msg) {
        this.t0 = System.currentTimeMillis();
        this.collectedBoids = new ArrayList<>();
        var boidsCopy = List.copyOf(this.boids);
        for (var boidActor : boidsActors) {
            boidActor.tell(new UpdateVelocityMsg(getSelf(), boidsCopy), getSelf());
        }
        getContext().become(receiverVelocities());
    }

    private Receive receiverVelocities() {
        return receiveBuilder()
                .match(VelocityUpdatedMsg.class, this::onVelocityUpdated)
                .matchAny(o -> stash())
                .build();
    }

    private void onVelocityUpdated(VelocityUpdatedMsg msg) {
        collectedBoids.add(msg.boid());
        if (collectedBoids.size() == boidsActors.size()) {
            this.boids = new ArrayList<>(collectedBoids);
            this.collectedBoids.clear();
            for (var boidActor : boidsActors) {
                boidActor.tell(new UpdatePositionMsg(), getSelf());
            }
            getContext().become(receiverPositions());
        }
    }

    private Receive receiverPositions() {
        return receiveBuilder()
                .match(SendBoidMsg.class, this::onBoidUpdated)
                .matchAny(o -> stash())
                .build();
    }

    private void onBoidUpdated(SendBoidMsg msg) {
        collectedBoids.add(msg.boid());
        if (collectedBoids.size() == boidsActors.size()) {
            this.boids = new ArrayList<>(collectedBoids);

            if (view.isPresent()) {
                view.get().update(framerate, this.boids);
            }

            var t1 = System.currentTimeMillis();
            var dtElapsed = t1 - t0;
            var frameratePeriod = 1000 / FRAMERATE;
            long delay = Math.max(0, frameratePeriod - dtElapsed);
            framerate = dtElapsed < frameratePeriod ? FRAMERATE : (int) (1000 / dtElapsed);

            getContext().getSystem().getScheduler().scheduleOnce(
                    Duration.ofMillis(delay),
                    getSelf(),
                    new RunSimulationMsg(),
                    getContext().getDispatcher(),
                    getSelf()
            );

            getContext().become(receiverRun());
            unstashAll();
        }
    }

    private void onPauseSimulator(PauseSimulationMsg msg) {
        getContext().become(receiverResume());
    }

    private Receive receiverResume() {
        return receiveBuilder()
                .match(ResumeSimulationMsg.class, this::onResumeSimulator)
                .match(StopSimulationMsg.class, this::onStopSimulator)
                .matchAny(o -> stash())
                .build();
    }

    private void onResumeSimulator(ResumeSimulationMsg msg) {
        unstashAll();
        getContext().become(receiverRun());
        getSelf().tell(new RunSimulationMsg(), getSelf());
    }

    public void onStopSimulator(StopSimulationMsg msg) {
        boidsActors.forEach(a -> a.tell(new StopMsg(), this.getSelf()));
        boids.clear();
        boidsActors.clear();
        this.unstashAll();
        this.getContext().become(receiverStart());
    }

    private void onStopMsg(StopSimulatorMsg msg) {
       // log("StopSimulatorMsg received");
        this.getContext().stop(this.getSelf());
    }

    private void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ " + this.getSelf().path().name() + " ] " + msg);
    }
}
