package it.unibo.agar.model;

import java.rmi.RemoteException;
import java.util.*;
import it.unibo.agar.view.LocalView;

public class PlayerControllerImpl implements PlayerController {

    private Optional<MainController> mainControllerStubOpt = Optional.empty();
    private List<PlayerController> playerControllerStubs = new ArrayList<>();
    private Optional<DistributedGameStateManager> distributedGameStateManagerOpt = Optional.empty();
    private Optional<String> localPlayerIdOpt = Optional.empty();
    private Optional<LocalView> localViewOpt = Optional.empty();

    @Override
    public void boot(MainController mainControllerStub, List<PlayerController> playerControllerStubs, World world, Player player) throws RemoteException {
        mainControllerStubOpt = Optional.of(mainControllerStub);
        this.playerControllerStubs = playerControllerStubs;
        localPlayerIdOpt = Optional.of(player.getId());

        // update player to mainController
        // update player to playerControllerStubs

        distributedGameStateManagerOpt = Optional.of(new DistributedGameStateManager(world, player, mainControllerStub, playerControllerStubs));
    }

    @Override
    public void sendActors(List<PlayerController> playerStubs) throws RemoteException {

    }

    @Override
    public void updatePlayer(Player player) throws RemoteException {

    }

    @Override
    public void eatPlayer(List<Player> players) throws RemoteException {

    }

    @Override
    public void eatFoods(List<Food> eatenFoods, List<Food> newFoods) throws RemoteException {

    }

    @Override
    public void tick() throws RemoteException {

    }

    @Override
    public void terminate() throws RemoteException {

    }
}
