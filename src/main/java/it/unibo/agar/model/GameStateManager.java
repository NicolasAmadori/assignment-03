package it.unibo.agar.model;

public interface GameStateManager {

    World getWorld();

    void setWorld(World world);

    void movePlayerDirection(final double dx, final double dy);

    void tick();

}
