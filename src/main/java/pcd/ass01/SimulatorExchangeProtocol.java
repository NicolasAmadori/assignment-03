package pcd.ass01;

import java.util.List;

import akka.actor.ActorRef;
import pcd.ass01.BoidsModel;
import pcd.ass01.Boid;

public class SimulatorExchangeProtocol {

    public record BootSimulationMsg(BoidsModel model) {}
    public record AttachViewMsg(BoidsView view) {}
    public record ResumeSimulationMsg() {}
    public record PauseSimulationMsg() {}
    public record StartSimulationMsg(int nBoids) {}
    public record StopSimulationMsg() {}
    public record RunSimulationLoopMsg() {}
    public record RunSimulationMsg() {}
}
