package it.unibo.agar.model;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainControllerImpl implements MainController {

    private World world;
    private List<PlayerController> playerStubs;
    private static int playerCounter = 0;
    private MainController selfStub;

    public MainControllerImpl(int width, int height, int numFoods, int maxMass){
        world = new World(width, height, maxMass, List.of(), GameInitializer.initialFoods(numFoods, width, height));
        playerStubs = new ArrayList<>();
    }

    @Override
    public void setSelfStub(MainController selfStub) throws RemoteException {
        this.selfStub = selfStub;
    }

    @Override
    public void connect(String playerName, PlayerController playerStub) throws RemoteException {
        if (selfStub == null) {
            throw new RemoteException();
        }

        Player newPlayer = GameInitializer.initialPlayer(playerName + "#" + playerCounter++, world.getWidth(), world.getHeight());
        log("Ricevuto connessione da " + newPlayer.getId());
        world = world.updatePlayer(newPlayer);

        playerStub.boot(selfStub, playerStubs, world, newPlayer);

        playerStubs.add(playerStub);
        for (PlayerController p : playerStubs) {
            if (!p.equals(playerStub)) {
                p.sendActors(playerStubs.stream()
                        .filter(p2 -> !p2.equals(p))
                        .toList()
                );
            }
        }
    }

    @Override
    public void updatePlayer(Player player) throws RemoteException {
        world = world.updatePlayer(player);
    }

    @Override
    public void eatFoods(List<Food> eatenFoods, List<Food> newFoods) throws RemoteException {
        world = world.removeFoods(eatenFoods).addFoods(newFoods);
    }

    @Override
    public void eatPlayers(List<Player> players) throws RemoteException {
        world = world.removePlayers(players);
    }

    @Override
    public void disconnect(PlayerController playerStub, String playerId) throws RemoteException {
        log("Ricevuto disconnect da " + playerId);
        Optional<Player> playerToRemove = world.getPlayerById(playerId);
        if (playerToRemove.isPresent()) {
            world = world.removePlayers(List.of(playerToRemove.get()));
        }

        playerStubs.remove(playerStub);

        for (PlayerController p : playerStubs) {
            p.sendActors(playerStubs.stream()
                    .filter(p2 -> !p2.equals(p))
                    .toList()
            );
        }
    }

    private void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ MainController ] " + msg);
    }
}
