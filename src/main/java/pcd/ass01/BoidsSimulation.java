package pcd.ass01;

import java.util.ArrayList;
import akka.actor.*;

import pcd.ass01.actors.BoidActor;
import static pcd.ass01.actors.BoidExchangeProtocol.*;
import static pcd.ass01.SimulatorExchangeProtocol.*;

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
		ActorSystem system = ActorSystem.create("boid-system");

		var model = new BoidsModel(
				SEPARATION_WEIGHT, ALIGNMENT_WEIGHT, COHESION_WEIGHT,
				ENVIRONMENT_WIDTH, ENVIRONMENT_HEIGHT,
				MAX_SPEED,
				PERCEPTION_RADIUS,
				AVOID_RADIUS);

		ActorRef sim = system.actorOf(Props.create(BoidsSimulator.class), "BoidsSimulatorActor");
		sim.tell(new BootSimulationMsg(model), ActorRef.noSender());

		var view = new BoidsView(model, sim, SCREEN_WIDTH, SCREEN_HEIGHT);
		sim.tell(new AttachViewMsg(view), ActorRef.noSender());
//		sim.tell(new RunSimulationLoopMsg(), ActorRef.noSender());
	}
}