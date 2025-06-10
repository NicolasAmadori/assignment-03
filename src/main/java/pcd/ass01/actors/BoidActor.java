package pcd.ass01.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

public class BoidActor {

    public static final class Greet {
        public final String whom;
        public final ActorRef<Greeted> replyTo;

        public Greet(String whom, ActorRef<Greeted> replyTo) {
            this.whom = whom;
            this.replyTo = replyTo;
        }
    }

    public static final class Greeted {
        public final String whom;
        public final ActorRef<Greet> from;

        public Greeted(String whom, ActorRef<Greet> from) {
            this.whom = whom;
            this.from = from;
        }
    }

    // Actor behavior definition
    public static Behavior<Greet> create() {
        return Behaviors.receive(Greet.class)
                .onMessage(Greet.class, BoidActor::onGreet)
                .build();
    }

    private static Behavior<Greet> onGreet(Greet message) {
        return Behaviors.setup(context -> {
            System.out.println("Hello " + message.whom + "!");
            message.replyTo.tell(new Greeted(message.whom, context.getSelf()));
            return Behaviors.same();
        });
    }
}
