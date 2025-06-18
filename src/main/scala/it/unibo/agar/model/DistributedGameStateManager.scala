package it.unibo.agar.model

import akka.actor.typed.ActorRef

import scala.util.Random

trait GameStateManager:

  def movePlayerDirection(dx: Double, dy: Double): Unit
  def tick() : Unit

class DistributedGameStateManager(
    var world: World,
    val player: Player,
    val mainActor: ActorRef[MainActor.MainActorMessage],
    val actors: Seq[ActorRef[PlayerActor.PlayerActorMessage]],
    val speed: Double = 10.0
) extends GameStateManager:

  private var deltaX: Double = 0.0
  private var deltaY: Double = 0.0
  world = world.updatePlayer(player)

  // Move a player in a given direction (dx, dy)
  def movePlayerDirection(dx: Double, dy: Double): Unit =
    deltaX = dx
    deltaY = dy

  def tick(): Unit =
    updatePlayerPosition()
    updateWorldAfterMovement()

  private def updatePlayerPosition(): Player =
    val newX = (player.x + deltaX * speed).max(0).min(world.width)
    val newY = (player.y + deltaY * speed).max(0).min(world.height)
    player.copy(x = newX, y = newY)

  private def updateWorldAfterMovement(): World =
    val foodEaten = world.foods.filter(food => EatingManager.canEatFood(player, food))
    val playerEatsFood = foodEaten.foldLeft(player)((p, food) => p.grow(food))
    val newFoods = foodEaten.map(food => Food(food.id, Random.nextInt(world.getWidth), Random.nextInt(world.getHeight), food.mass))
    mainActor ! MainActor.EatFoods(foodEaten, newFoods)
    actors.foreach(_ ! PlayerActor.EatFoods(foodEaten, newFoods))

    val playersEaten = world
      .playersExcludingSelf(player)
      .filter(player => EatingManager.canEatPlayer(playerEatsFood, player))

    val playerEatPlayers = playersEaten.foldLeft(playerEatsFood)((p, other) => p.grow(other))
    mainActor ! MainActor.EatPlayers(playersEaten)
    actors.foreach(_ ! PlayerActor.EatPlayers(playersEaten))

    mainActor ! MainActor.UpdatePlayer(playerEatPlayers)
    actors.foreach(_ ! PlayerActor.UpdatePlayer(playerEatPlayers))

    world
      .updatePlayer(playerEatPlayers)
      .removePlayers(playersEaten)
      .removeFoods(foodEaten)
      .addFoods(newFoods)
