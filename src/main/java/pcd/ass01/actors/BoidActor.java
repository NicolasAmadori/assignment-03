package pcd.ass01.actors;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;

import pcd.ass01.Boid;
import static pcd.ass01.actors.BoidExchangeProtocol.*;

public class BoidActor extends AbstractActorWithStash {

    private Boid boid;
    private ActorRef simulatorActor;

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
        this.getContext().become(receiverUpdate());
    }

    /**
     * Behaviour to receive updates
     * A stop message can be received to stop the actor
     */
    public Receive receiverUpdate() {
        return receiveBuilder()
                .match(UpdateVelocityMsg.class, this::onUpdateVelocity)
                .match(UpdatePositionMsg.class, this::onUpdatePosition)
                .match(StopMsg.class, this::onStopMsg)
                .build();
    }

    private void onUpdateVelocity(UpdateVelocityMsg msg) {
        this.simulatorActor = msg.replyTo();
        boid.updateVelocity(msg.boids());
        simulatorActor.tell(new VelocityUpdatedMsg(boid), getSelf());
    }

    private void onUpdatePosition(UpdatePositionMsg msg) {
        boid.updatePos();
        simulatorActor.tell(new SendBoidMsg(boid), getSelf());
    }

    private void onStopMsg(StopMsg msg) {
       // log("StopMsg received");
        this.getContext().stop(this.getSelf());
    }

    private void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ " + this.getSelf().path().name() + " ] " + msg);
    }

}
