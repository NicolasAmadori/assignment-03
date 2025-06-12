package pcd.ass01.actors;

import java.util.ArrayList;
import java.util.List;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;

import pcd.ass01.Boid;
import static pcd.ass01.actors.BoidExchangeProtocol.*;

public class BoidActor extends AbstractActor {

    private Boid boid;
    private List<Boid> boids;
    private List<ActorRef> actorBoids;
    private ActorRef viewActor;
    private Receive lastReceive;

    /**
     * Behaviour to wait for a BootMsg to create the boid.
     * A stop message can be received to stop the actor
     */
    public Receive createReceive() {
        return receiveBuilder()
                .match(BootMsg.class, this::onBootMsg)
                .match(StopMsg.class, this::onStopMsg)
                .build();
    }

    private void onBootMsg(BootMsg msg) {
        log("BootMsg received");
        this.actorBoids = msg.actorBoids();
        this.boid = new Boid(msg.model());
        this.boids = new ArrayList<>();
        lastReceive = receiverUpdate();
        this.getContext().become(lastReceive);
    }

    /**
     * Behaviour to receive updates
     * A stop message can be received to stop the actor
     */
    public Receive receiverUpdate() {
        return receiveBuilder()
                .match(UpdateMsg.class, this::onUpdate)
                .match(StopMsg.class, this::onStopMsg)
                .build();
    }

    private void onUpdate(UpdateMsg msg) {
        log("UpdateMsg received");
        viewActor = msg.replyTo();
        actorBoids.forEach(actor -> {
            actor.tell(new SendBoidMsg(boid), this.getSelf());
        });
        lastReceive = receiverBoids();
        this.getContext().become(lastReceive);
    }

    /**
     * Behaviour to only receive Boids by the other actors
     * A stop message can be received to stop the actor
     */
    public Receive receiverBoids() {
        return receiveBuilder()
                .match(SendBoidMsg.class, this::onSendBoidMsg)
                .match(StopMsg.class, this::onStopMsg)
                .match(PauseMsg.class, this::onPauseMsg)
                .build();
    }

    private void onSendBoidMsg(SendBoidMsg msg) {
        log("SendBoidMsg received");
        boids.add(msg.boid());
        if (boids.size() == actorBoids.size()) {
            log("All boids received by the other actors");
            boid.update(boids);
            boids.clear();
            viewActor.tell(new SendBoidMsg(boid), this.getSelf());
            lastReceive = receiverUpdate();
            this.getContext().become(lastReceive);
        }
    }

    private void onPauseMsg(PauseMsg msg) {
        log("PauseMsg received");
        this.getContext().become(receiverResume());
    }

    /**
     * Behaviour to wait for a Resume message after pausing
     * A stop message can be received anytime to stop the actor
     */
    public Receive receiverResume() {
        return receiveBuilder()
                .match(ResumeMsg.class, this::onResumeMsg)
                .match(StopMsg.class, this::onStopMsg)
                .build();
    }

    private void onResumeMsg(ResumeMsg msg) {
        log("ResumeMsg received");
        this.getContext().become(lastReceive);
    }

    private void onStopMsg(StopMsg msg) {
        this.getContext().stop(this.getSelf());
    }

    private void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ " + this.getSelf().path().name() + " ] " + msg);
    }

}
