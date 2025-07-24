package it.unibo.agar.controller;

import it.unibo.agar.model.Food;
import it.unibo.agar.model.Player;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface MainController extends Remote {

    void setSelfStub(MainController selfStub) throws RemoteException;

    void connect(String playerName, PlayerController playerStub) throws RemoteException;

    void updatePlayer(Player player) throws RemoteException;

    void eatFoods(List<Food> eatenFoods, List<Food> newFoods) throws RemoteException;

    void eatPlayers(List<Player> players) throws RemoteException;

    void disconnect(PlayerController playerStub, String playerId) throws RemoteException;

}
