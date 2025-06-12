package pcd.ass01.actors;

import java.util.List;

import akka.actor.ActorRef;
import pcd.ass01.BoidsModel;
import pcd.ass01.Boid;

public class BoidExchangeProtocol {

    public record BootMsg(List<ActorRef> actorBoids, BoidsModel model) {}

    public record UpdateMsg(ActorRef replyTo) {}
    public record SendBoidMsg(Boid boid) {}
    public record RequestBoidMsg(ActorRef replyTo) {}

    public record PauseMsg() {}
    public record ResumeMsg() {}

    public record StopMsg() {}
}
