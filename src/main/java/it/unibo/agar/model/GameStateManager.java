package it.unibo.agar.model;

public interface GameStateManager {

    World getWorld();

    void movePlayerDirection(final double dx, final double dy);

    void tick();

}
