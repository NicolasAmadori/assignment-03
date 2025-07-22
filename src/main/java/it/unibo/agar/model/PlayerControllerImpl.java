package it.unibo.agar.model;

import java.rmi.RemoteException;
import java.util.*;
import it.unibo.agar.view.LocalView;

public class PlayerControllerImpl implements PlayerController {

    private MainController mainControllerStub;
    private List<PlayerController> playerControllerStubs;
    private DistributedGameStateManager distributedGameStateManager;
    private String localPlayerId;
    private LocalView localView;

    @Override
    public void boot(MainController mainControllerStub, List<PlayerController> playerControllerStubs, World world, Player player) throws RemoteException {
        this.mainControllerStub = mainControllerStub;
        this.playerControllerStubs = playerControllerStubs;
        localPlayerId = player.getId();

        // update player to playerControllerStubs
        playerControllerStubs.forEach(p -> {
            try {
                p.updatePlayer(player);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });

        distributedGameStateManager = new DistributedGameStateManager(world, player, mainControllerStub, playerControllerStubs);
        localView = new LocalView(distributedGameStateManager, player.getId(), this); //TODO: devo mandare lo stub e quindi settarlo come fa il mainController?
    }

    @Override
    public void sendActors(List<PlayerController> playerControllerStubs) throws RemoteException {
//        distributedGameStateManager.ifPresent(dgsm -> {
//            // Update the playerControllerStubs in the DistributedGameStateManager
//            dgsm.setPlayerControllerStubs(playerControllerStubs);
//            // Notify the local view to update the player list
//            localView.ifPresent(view -> view.updatePlayerList(playerControllerStubs));
//        });
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
