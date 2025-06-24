package pcd.ass01.actors;

import java.util.List;

import akka.actor.ActorRef;
import pcd.ass01.BoidsModel;
import pcd.ass01.Boid;

public class BoidExchangeProtocol {

    public record BootMsg(List<ActorRef> actorBoids, Boid boid) {}

    public record UpdateVelocityMsg(ActorRef replyTo, List<Boid> boids) {}
    public record VelocityUpdatedMsg(Boid boid) {}
    public record UpdatePositionMsg() {}
    public record UpdateMsg(ActorRef replyTo, List<Boid> boids) {}
    public record SendBoidMsg(Boid boid) {}

    public record PauseMsg() {}
    public record ResumeMsg() {}

    public record StopMsg() {}
}
