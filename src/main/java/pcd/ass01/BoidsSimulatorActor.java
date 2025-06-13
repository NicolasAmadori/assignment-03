package pcd.ass01;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;
import pcd.ass01.actors.BoidActor;
import static pcd.ass01.actors.BoidExchangeProtocol.*;
import static pcd.ass01.SimulatorExchangeProtocol.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BoidsSimulatorActor extends AbstractActorWithStash {

    private BoidsModel model;
    private Optional<BoidsView> view;
    private static final int FRAMERATE = 25;
    private int framerate;
    private List<Boid> boids = new ArrayList<>();
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

    public Receive receiverStart() {
        return receiveBuilder()
                .match(StartSimulationMsg.class, this::onStartSimulator)
                .match(StopSimulatorMsg.class, this::onStopMsg)
                .build();
    }

    public Receive receiverUpdate() {
        return receiveBuilder()
                .match(StartSimulationMsg.class, (msg) -> { this.stash(); })
                .match(RunSimulationMsg.class, this::onRunSimulation)
                .match(SendBoidMsg.class, this::onSendBoidMsg)
                .match(PauseSimulationMsg.class, this::onPauseSimulator)
                .match(ResumeSimulationMsg.class, this::onResumeSimulator)
                .match(StopSimulationMsg.class, this::onStopSimulator)
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

    public void onResumeSimulator(ResumeSimulationMsg msg) {
        boidsActors.forEach(a -> a.tell(new ResumeMsg(), this.getSelf()));
        this.getSelf().tell(new RunSimulationMsg(), this.getSelf());
    }

    public void onPauseSimulator(PauseSimulationMsg msg) {
        boidsActors.forEach(a -> a.tell(new PauseMsg(), this.getSelf()));
    }

    public void onStartSimulator(StartSimulationMsg msg) {
        for (int i = 0; i < msg.nBoids(); i++) {
            var boidActor = this.getContext().actorOf(Props.create(BoidActor.class), "B-" + i);
            boidsActors.add(boidActor);
            var boid = new Boid(model);
            boids.add(boid);
            boidActor.tell(new BootMsg(boidsActors, boid), this.getSelf());
        }
        this.getContext().become(receiverUpdate());
        this.getSelf().tell(new RunSimulationMsg(), this.getSelf());
    }

    public void onStopSimulator(StopSimulationMsg msg) {
        boidsActors.forEach(a -> a.tell(new StopMsg(), this.getSelf()));
        boids.clear();
        boidsActors.clear();
        this.unstashAll();
        this.getContext().become(receiverStart());
    }

    private void onRunSimulation(RunSimulationMsg msg) {
        t0 = System.currentTimeMillis();
        var boidsCopy = List.copyOf(boids);
        for (var boidActor : boidsActors) {
            boidActor.tell(new UpdateMsg(this.getSelf(), boidsCopy), this.getSelf());
        }
        boids.clear();
    }

    private void onSendBoidMsg(SendBoidMsg msg) {
       // log("SendBoidMsg received");
        boids.add(msg.boid());

        if (boids.size() == boidsActors.size()) {
           // log("Received all the updated positions by the boid actors");
            if (view.isPresent()) {
                view.get().update(framerate, List.copyOf(boids));
            }
            var t1 = System.currentTimeMillis();
            var dtElapsed = t1 - t0;
            var frameratePeriod = 1000/FRAMERATE;
            long delay = Math.max(0, frameratePeriod - dtElapsed);
            framerate = dtElapsed < frameratePeriod ? FRAMERATE : (int) (1000/dtElapsed);

            //Invia un messaggio a se stesso aspettando il delay del framerate
            getContext().getSystem().scheduler().scheduleOnce(
                    java.time.Duration.ofMillis(delay),
                    getSelf(),
                    new RunSimulationMsg(),
                    getContext().getDispatcher(),
                    getSelf()
            );
        }
    }

    private void onStopMsg(StopSimulatorMsg msg) {
       // log("StopSimulatorMsg received");
        this.getContext().stop(this.getSelf());
    }

    private void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ " + this.getSelf().path().name() + " ] " + msg);
    }
}
