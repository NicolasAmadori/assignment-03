package pcd.ass01;


import akka.actor.typed.javadsl.Behaviors;
import pcd.ass01.actors.BoidActor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import pcd.ass01.actors.PingPongerJava;

public class BoidsSimulation {

	final static double SEPARATION_WEIGHT = 1.0;
	final static double ALIGNMENT_WEIGHT = 1.0;
	final static double COHESION_WEIGHT = 1.0;

	final static int ENVIRONMENT_WIDTH = 1000;
	final static int ENVIRONMENT_HEIGHT = 1000;
	static final double MAX_SPEED = 4.0;
	static final double PERCEPTION_RADIUS = 50.0;
	static final double AVOID_RADIUS = 20.0;

	final static int SCREEN_WIDTH = 800;
	final static int SCREEN_HEIGHT = 800;

	public static void main(String[] args) {
//		var model = new BoidsModel(
//				SEPARATION_WEIGHT, ALIGNMENT_WEIGHT, COHESION_WEIGHT,
//				ENVIRONMENT_WIDTH, ENVIRONMENT_HEIGHT,
//				MAX_SPEED,
//				PERCEPTION_RADIUS,
//				AVOID_RADIUS);
//		var sim = new BoidsSimulator(model);
//
//		var view = new BoidsView(model, sim, SCREEN_WIDTH, SCREEN_HEIGHT);
//		sim.attachView(view);
//		sim.runSimulationLoop();

//		ActorSystem<BoidActor.Greet> system = ActorSystem.create(BoidActor.create(), "hello-world");
//		system.tell(new BoidActor.Greet("Akka Typed", system.ignoreRef()));
//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		system.terminate();

//		ActorRef<PingPongerJava.PingPongJava> pingPonger = ActorSystem.create(PingPongerJava.create(10), "pingPonger");
//		pingPonger.tell(new PingPongerJava.PingPongJava.Ping(pingPonger.unsafeUpcast()));

		ActorSystem<Void> system = ActorSystem.create(Behaviors.setup(context -> {
			ActorRef<PingPongerJava.PingPongJava> pingActor = context.spawn(PingPongerJava.create(10), "pingActor");
			ActorRef<PingPongerJava.PingPongJava> pongActor = context.spawn(PingPongerJava.create(10), "pongActor");

			// Inizializza la sequenza di ping-pong
			pingActor.tell(new PingPongerJava.PingPongJava.Ping(pongActor));

			return Behaviors.empty();
		}), "PingPongSystem");
	}
}