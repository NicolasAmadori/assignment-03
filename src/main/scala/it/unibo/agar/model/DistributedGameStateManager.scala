package it.unibo.agar.model

import akka.actor.typed.ActorRef

import scala.util.Random

trait GameStateManager:

  def getWorld: World
  def movePlayerDirection(dx: Double, dy: Double): Unit

class DistributedGameStateManager(
    var world: World,
    val player: Player,
    val playerActor: ActorRef[PlayerActor.PlayerActorMessage],
    val speed: Double = 10.0
) extends GameStateManager:

  private var deltaX: Double = 0.0
  private var deltaY: Double = 0.0
  def getWorld: World = world

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
    val newFoods = foodEaten.map(food => {
      val newFood = Food(food.id, Random.nextInt(world.getWidth), Random.nextInt(world.getHeight), food.mass)
      playerActor ! PlayerActor.EatFood(food, newFood)
      newFood
    })

    val playersEaten = world
      .playersExcludingSelf(player)
      .filter(player => EatingManager.canEatPlayer(playerEatsFood, player))
    val playerEatPlayers = playersEaten.foldLeft(playerEatsFood)((p, other) => p.grow(other))
    playersEaten.foreach(p => playerActor ! PlayerActor.EatPlayer(p))

    playerActor ! PlayerActor.UpdatePlayer(player)
    world
      .updatePlayer(playerEatPlayers)
      .removePlayers(playersEaten)
      .removeFoods(foodEaten)
      .addFoods(newFoods)


//  def updatePlayer(player: Player): Unit =
//    val w = getWorld
//    if (w.players.exists(p => p.id == player.id)) {
//      val updatedPlayers = w.players.updated(
//        w.players.indexWhere(p => p.id == player.id),
//        player
//      )
//      w.copy(players = updatedPlayers)
//      playerActor ! PlayerActor.UpdatePlayer(player)
//    }
//
//  def eatPlayer(player: Player): Unit =
//    val w = getWorld
//    val updatedPlayers = w.players.filterNot(p => p.id == player.id)
//    w.copy(players = updatedPlayers)
//    playerActor ! PlayerActor.EatPlayer(player)
//
//  def eatFood(eatenFood: Food, newFood: Food): Unit =
//    val w = getWorld
//    val updatedFoods = w.foods.filterNot(f => f.id == eatenFood.id) :+ newFood
//    w.copy(foods = updatedFoods)
//    playerActor ! PlayerActor.EatFood(eatenFood, newFood)
