package it.unibo.agar.model;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class RunServerSide {

    static Integer WIDTH = 1000;
    static Integer HEIGHT = 1000;
    static Integer NUM_FOODS = 100;
    static Integer MAX_MASS = 4000;

    public static void main(String[] args) {

        try {
            var mainController = new MainControllerImpl(WIDTH, HEIGHT, NUM_FOODS, MAX_MASS);

            var mainControllerStub = (MainController) UnicastRemoteObject.exportObject(mainController, 0);
            mainController.setSelfStub(mainControllerStub);

            var registry = LocateRegistry.getRegistry();
            registry.rebind("mainController", mainControllerStub);

            log("MainController object registered.");
        } catch (Exception e) {
            log("Server exception: " + e);
            e.printStackTrace();
        }
    }

    private static void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ Main ] " + msg);
    }
}
