package it.unibo.agar.model;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface PlayerController extends Remote {

    void setSelfStub(PlayerController selfStub) throws RemoteException;

    void boot(MainController mainController, List<PlayerController> playerStubs, World world, Player player) throws RemoteException;

    void sendPlayerStubs(List<PlayerController> playerStubs) throws RemoteException;

    void updatePlayer(Player player) throws RemoteException;

    void eatPlayer(List<Player> players) throws RemoteException;

    void eatFoods(List<Food> eatenFoods, List<Food> newFoods) throws RemoteException;

    void tick() throws RemoteException;

    void terminate(boolean closingView) throws RemoteException;
}
