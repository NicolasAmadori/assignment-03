package it.unibo.agar.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class World {
    private final int width;
    private final int height;
    private final int maxMass;
    private final List<Player> players;
    private final List<Food> foods;

    public World(int width, int height, int maxMass, List<Player> players, List<Food> foods) {
        this.width = width;
        this.height = height;
        this.maxMass = maxMass;
        this.players = List.copyOf(players); // Ensure immutability
        this.foods = List.copyOf(foods);     // Ensure immutability
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<Food> getFoods() {
        return foods;
    }

    public List<Player> getPlayersExcludingSelf(final Player player) {
        return players.stream()
                .filter(p -> !p.getId().equals(player.getId()))
                .collect(Collectors.toList());
    }

    public Optional<Player> getPlayerById(final String id) {
        return players.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }

    public World updatePlayer(final Player player) {
        List<Player> newPlayers = players.stream()
                .map(p -> p.getId().equals(player.getId()) ? player : p)
                .collect(Collectors.toList());
        return new World(width, height, maxMass, newPlayers, foods);
    }


    public World removePlayers(final List<Player> playersToRemove) {
        List<String> idsToRemove = playersToRemove.stream().map(Player::getId).toList();
        List<Player> newPlayers = players.stream()
                .filter(p -> !idsToRemove.contains(p.getId()))
                .collect(Collectors.toList());
        return new World(width, height, maxMass, newPlayers, foods);
    }

    public World removeFoods(List<Food> foodsToRemove) {
        List<Food> newFoods = foods.stream()
                .filter(f -> !foodsToRemove.contains(f)) // Assumes Food has proper equals/hashCode or relies on object identity if not overridden
                .collect(Collectors.toList());
        return new World(width, height, maxMass, players, newFoods);
    }

    public World addFoods(List<Food> foodsToAdd) {
        List<Food> newFoods = Stream.concat(this.foods.stream(), foodsToAdd.stream())
                .collect(Collectors.toList());
        return new World(width, height, maxMass, players, newFoods);
    }
}
