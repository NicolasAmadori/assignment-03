package it.unibo.agar.model;

import java.rmi.RemoteException;
import java.util.*;
import it.unibo.agar.view.LocalView;

public class PlayerControllerImpl implements PlayerController {

    private MainController mainControllerStub;
    private List<PlayerController> playerStubs;
    private DistributedGameStateManager distributedGameStateManager;
    private String localPlayerId;
    private LocalView localView;
    private PlayerController selfStub;
    private Timer timer;

    public void startTicking() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    selfStub.tick();
                } catch (Exception e) {
                    log("Tick error: " + e.getMessage());
                }
            }
        }, 30, 30);
    }

    @Override
    public void setSelfStub(PlayerController selfStub) {
        this.selfStub = selfStub;
    }

    @Override
    public void boot(MainController mainControllerStub, List<PlayerController> playerStubs, World world, Player player) throws RemoteException {
        this.mainControllerStub = mainControllerStub;
        this.playerStubs = playerStubs;
        localPlayerId = player.getId();

        // update player to playerControllerStubs
        for (var p : playerStubs) {
            p.updatePlayer(player);
        }

        distributedGameStateManager = new DistributedGameStateManager(world, player, mainControllerStub, playerStubs);
        localView = new LocalView(distributedGameStateManager, player.getId(), selfStub);
        startTicking();
    }

    @Override
    public void sendActors(List<PlayerController> playerStubs) throws RemoteException {
        checkIfBooted();
        distributedGameStateManager.setPlayerStubs(playerStubs);
    }

    @Override
    public void updatePlayer(Player player) throws RemoteException {
        checkIfBooted();
        if (player.getMass() >= distributedGameStateManager.getWorld().getMaxMass()) {
            mainControllerStub.disconnect(selfStub, localPlayerId);
            localView.showMessage(player.getId() + "HAS WON THE GAME");
        } else {
            distributedGameStateManager.setWorld(distributedGameStateManager.getWorld().updatePlayer(player));
        }
    }

    @Override
    public void eatPlayer(List<Player> players) throws RemoteException {
        checkIfBooted();
        if (players.stream().map(Player::getId).toList().contains(localPlayerId)) {
            mainControllerStub.disconnect(selfStub, localPlayerId);
            localView.closeView();
        } else {
            distributedGameStateManager.setWorld(distributedGameStateManager.getWorld().removePlayers(players));
        }
    }

    @Override
    public void eatFoods(List<Food> eatenFoods, List<Food> newFoods) throws RemoteException {
        checkIfBooted();
        distributedGameStateManager.setWorld(distributedGameStateManager.getWorld().removeFoods(eatenFoods).addFoods(newFoods));
    }

    @Override
    public void tick() throws RemoteException {
        checkIfBooted();
        var p = distributedGameStateManager.getWorld().getPlayerById(localPlayerId);
        if (p.isPresent() && p.get().getMass() >= distributedGameStateManager.getWorld().getMaxMass()) {
            mainControllerStub.disconnect(selfStub, localPlayerId);
            localView.showMessage("CONGRATULATIONS " + localPlayerId + ", YOU WON!");
        }
        distributedGameStateManager.tick();
        localView.repaintView();
    }

    @Override
    public void terminate() throws RemoteException {
        checkIfBooted();
        if (timer != null) {
            timer.cancel();
        }
        mainControllerStub.disconnect(selfStub, localPlayerId);
        localView.closeView();
    }

    private void checkIfBooted() throws RemoteException {
        if (mainControllerStub == null) {
            throw new RemoteException("mainControllerStub is null");
        }

        if (playerStubs == null) {
            throw new RemoteException("playerControllerStubs is null");
        }

        if (distributedGameStateManager == null) {
            throw new RemoteException("distributedGameStateManager is null");
        }

        if (localView == null) {
            throw new RemoteException("localView is null");
        }
    }

    private void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ " + localPlayerId + " ] " + msg);
    }
}
