package pcd.ass01.actors;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;

import pcd.ass01.Boid;
import static pcd.ass01.actors.BoidExchangeProtocol.*;

public class BoidActor extends AbstractActorWithStash {

    private Boid boid;
    private ActorRef simulatorActor;
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
       // log("BootMsg received");
        this.boid = msg.boid();
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
                .match(PauseMsg.class, this::onPauseMsg)
                .match(ResumeMsg.class, (msg) -> { this.stash(); })
                .match(StopMsg.class, this::onStopMsg)
                .build();
    }

    private void onUpdate(UpdateMsg msg) {
       // log("UpdateMsg received");
        simulatorActor = msg.replyTo();
        boid.update(msg.boids());
        simulatorActor.tell(new SendBoidMsg(boid), this.getSelf());
    }

    private void onPauseMsg(PauseMsg msg) {
       // log("PauseMsg received");
        this.unstashAll();
        this.getContext().become(receiverResume());
    }

    /**
     * Behaviour to wait for a Resume message after pausing
     * A stop message can be received anytime to stop the actor
     */
    public Receive receiverResume() {
        return receiveBuilder()
                .match(ResumeMsg.class, this::onResumeMsg)
                .match(UpdateMsg.class, (msg) -> { this.stash(); })
                .match(StopMsg.class, this::onStopMsg)
                .build();
    }

    private void onResumeMsg(ResumeMsg msg) {
       // log("ResumeMsg received");
        this.unstashAll();
        this.getContext().become(lastReceive);
    }

    private void onStopMsg(StopMsg msg) {
       // log("StopMsg received");
        this.getContext().stop(this.getSelf());
    }

    private void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ " + this.getSelf().path().name() + " ] " + msg);
    }

}
