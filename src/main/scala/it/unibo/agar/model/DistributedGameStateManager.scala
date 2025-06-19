package it.unibo.agar.model

import akka.actor.typed.ActorRef

import scala.util.Random

trait GameStateManager:

  def movePlayerDirection(dx: Double, dy: Double): Unit
  def tick() : Unit

class DistributedGameStateManager(
   var world: World,
   var initialPlayer: Player,
   val mainActor: ActorRef[MainActor.MainActorMessage],
   var actors: Seq[ActorRef[PlayerActor.PlayerActorMessage]],
   val speed: Double = 10.0
) extends GameStateManager:

  private var deltaX: Double = 0.0
  private var deltaY: Double = 0.0
  val playerId: String = initialPlayer.id
  world = world.updatePlayer(initialPlayer)

  // Move a player in a given direction (dx, dy)
  def movePlayerDirection(dx: Double, dy: Double): Unit =
    deltaX = dx
    deltaY = dy

  def tick(): Unit =
    world = world.updatePlayer(updatePlayerPosition())
    world = updateWorldAfterMovement()

  private def updatePlayerPosition(): Player =
    val newX = (world.playerById(playerId).get.x + deltaX * speed).max(0).min(world.width)
    val newY = (world.playerById(playerId).get.y + deltaY * speed).max(0).min(world.height)
    world.playerById(playerId).get.copy(x = newX, y = newY)

  private def updateWorldAfterMovement(): World =
    val foodEaten = world.foods.filter(food => EatingManager.canEatFood(world.playerById(playerId).get, food))
    val playerEatsFood = foodEaten.foldLeft(world.playerById(playerId).get)((p, food) => p.grow(food))
    val newFoods = foodEaten.map(food => Food(food.id, Random.nextInt(world.getWidth), Random.nextInt(world.getHeight), food.mass))
    if (foodEaten.nonEmpty)
      mainActor ! MainActor.EatFoods(foodEaten, newFoods)
      actors.foreach(_ ! PlayerActor.EatFoods(foodEaten, newFoods))

    val playersEaten = world
      .playersExcludingSelf(world.playerById(playerId).get)
      .filter(player => EatingManager.canEatPlayer(playerEatsFood, player))

    val playerEatPlayers = playersEaten.foldLeft(playerEatsFood)((p, other) => p.grow(other))

    if (playersEaten.nonEmpty)
      mainActor ! MainActor.EatPlayers(playersEaten)
      actors.foreach(_ ! PlayerActor.EatPlayers(playersEaten))

    mainActor ! MainActor.UpdatePlayer(playerEatPlayers)
    actors.foreach(_ ! PlayerActor.UpdatePlayer(playerEatPlayers))

    world
      .updatePlayer(playerEatPlayers)
      .removePlayers(playersEaten)
      .removeFoods(foodEaten)
      .addFoods(newFoods)
