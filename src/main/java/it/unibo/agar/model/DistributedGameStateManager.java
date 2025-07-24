package it.unibo.agar.model;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


public class DistributedGameStateManager implements GameStateManager {
    private static final double PLAYER_SPEED = 2.0;
    private static final int MAX_FOOD_ITEMS = 150;
    private static final Random random = new Random();
    private World world;
    private final Player initialPlayer;
    private final MainController mainController;
    private List<PlayerController> playerStubs;
    private final double speed;
    private double deltaX = 0.0;
    private double deltaY = 0.0;
    private String playerId;

    public DistributedGameStateManager(final World initialWorld, final Player initialPlayer, final MainController mainController, final List<PlayerController> playerStubs, final double speed) {
        this.world = initialWorld;
        this.initialPlayer = initialPlayer;
        this.mainController = mainController;
        this.playerStubs = new CopyOnWriteArrayList<>(playerStubs);
        this.speed = speed;

        playerId = initialPlayer.getId();
        this.world = this.world.updatePlayer(initialPlayer);
    }

    public DistributedGameStateManager(final World initialWorld, final Player initialPlayer, final MainController mainController, final List<PlayerController> playerStubs) {
        this(initialWorld, initialPlayer, mainController, playerStubs, 10.0);
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public void setWorld(World world) {
        this.world = world;
    }

    public void setPlayerStubs(final List<PlayerController> playerStubs) {
        this.playerStubs = Collections.unmodifiableList(playerStubs);
    }

    @Override
    public void movePlayerDirection(final double dx, final double dy) {
        deltaX = dx;
        deltaY = dy;
    }

    public void tick() {
        world = world.updatePlayer(updatePlayerPosition());
        world = updateWorldAfterMovement();
    }

    private Player updatePlayerPosition() {
        Optional<Player> playerOpt = world.getPlayerById(playerId);
        if (playerOpt.isEmpty()) {
            throw new IllegalStateException("Player not found");
        }

        Player player = playerOpt.get();
        final double newX = Math.max(0, Math.min(world.getWidth(), player.getX() + deltaX * speed));
        final double newY = Math.max(0, Math.min(world.getHeight(), player.getY() + deltaY * speed));
        return new Player(player.getId(), newX, newY, player.getMass());
    }

    private World updateWorldAfterMovement() {
        Optional<Player> playerOpt = world.getPlayerById(playerId);
        if (playerOpt.isEmpty()) {
            throw new IllegalStateException("Player not found");
        }

        final List<Food> eatenFood = world.getFoods().stream()
                .filter(food -> EatingManager.canEatFood(playerOpt.get(), food))
                .toList();
        final Player playerEatsFood = eatenFood.stream()
                .reduce(playerOpt.get(), Player::grow, (p1, p2) -> p1);
        final List<Food> newFoods = eatenFood.stream()
                .map(food -> new Food(
                        food.getId(),
                        random.nextInt(world.getWidth()),
                        random.nextInt(world.getHeight())
                ))
                .toList();

        if (!eatenFood.isEmpty()) {
            try {
                mainController.eatFoods(eatenFood, newFoods);
                playerStubs.forEach(p -> {
                    try {
                        p.eatFoods(eatenFood, newFoods);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        List<Player> playersEaten = world.getPlayersExcludingSelf(playerOpt.get()).stream()
                .filter(player -> EatingManager.canEatPlayer(playerEatsFood, player))
                .toList();

        Player playerEatsPlayers = playersEaten.stream()
                .reduce(playerEatsFood, Player::grow, (p1, p2) -> p1);

        if (!playersEaten.isEmpty()) {
            try {
                mainController.eatPlayers(playersEaten);
                for (PlayerController p : playerStubs) {
                    p.eatPlayer(playersEaten);
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            mainController.updatePlayer(playerEatsPlayers);
            for (PlayerController p : playerStubs) {
                p.updatePlayer(playerEatsPlayers);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        return world
                .updatePlayer(playerEatsPlayers)
                .removePlayers(playersEaten)
                .removeFoods(eatenFood)
                .addFoods(newFoods);
    }

//    private World moveAllPlayers(final World currentWorld) {
//        final List<Player> updatedPlayers = currentWorld.getPlayers().stream()
//                .map(player -> {
//                    Position direction = playerDirections.getOrDefault(player.getId(), Position.ZERO);
//                    final double newX = player.getX() + direction.x() * PLAYER_SPEED;
//                    final double newY = player.getY() + direction.y() * PLAYER_SPEED;
//                    return player.moveTo(newX, newY);
//                })
//                .collect(Collectors.toList());
//
//        return new World(currentWorld.getWidth(), currentWorld.getHeight(), updatedPlayers, currentWorld.getFoods());
//    }



//    private World handleEating(final World currentWorld) {
//        final List<Player> updatedPlayers = currentWorld.getPlayers().stream()
//                .map(player -> growPlayer(currentWorld, player))
//                .toList();
//
//        final List<Food> foodsToRemove = currentWorld.getPlayers().stream()
//                .flatMap(player -> eatenFoods(currentWorld, player).stream())
//                .distinct()
//                .toList();
//
//        final List<Player> playersToRemove = currentWorld.getPlayers().stream()
//                .flatMap(player -> eatenPlayers(currentWorld, player).stream())
//                .distinct()
//                .toList();
//
//        return new World(currentWorld.getWidth(), currentWorld.getHeight(), updatedPlayers, currentWorld.getFoods())
//                .removeFoods(foodsToRemove)
//                .removePlayers(playersToRemove);
//    }
//
//    private Player growPlayer(final World world, final Player player) {
//        final Player afterFood = eatenFoods(world, player).stream()
//                .reduce(player, Player::grow, (p1, p2) -> p1);
//
//        return eatenPlayers(world, afterFood).stream()
//                .reduce(afterFood, Player::grow, (p1, p2) -> p1);
//    }
//
//    private List<Food> eatenFoods(final World world, final Player player) {
//        return world.getFoods().stream()
//                .filter(food -> EatingManager.canEatFood(player, food))
//                .toList();
//    }
//
//    private List<Player> eatenPlayers(final World world, final Player player) {
//        return world.getPlayersExcludingSelf(player).stream()
//                .filter(other -> EatingManager.canEatPlayer(player, other))
//                .toList();
//    }

//    private void cleanupPlayerDirections() {
//        List<String> currentPlayerIds = this.world.getPlayers().stream()
//                .map(Player::getId)
//                .collect(Collectors.toList());
//
//        this.playerDirections.keySet().retainAll(currentPlayerIds);
//        this.world.getPlayers().forEach(p ->
//                playerDirections.putIfAbsent(p.getId(), Position.ZERO));
//    }

}
