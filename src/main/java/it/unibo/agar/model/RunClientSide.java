package it.unibo.agar.model;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class RunClientSide {

    static String PLAYER_NAME = "player";

    public static void main(String[] args) {

        String host = (args.length < 1) ? null : args[0];
        try {
            var registry = LocateRegistry.getRegistry(host);
            var mainController = (MainController) registry.lookup("mainController");

            var playerController = new PlayerControllerImpl();

            var playerControllerStub = (PlayerController) UnicastRemoteObject.exportObject(playerController, 0); //TODO: check port number correctness

            mainController.connect(PLAYER_NAME, playerControllerStub);

        } catch (Exception e) {
            log("Client exception: " + e);
            e.printStackTrace();
        }
    }

    private static void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ Client Main ] " + msg);
    }
}
