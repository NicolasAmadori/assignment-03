package it.unibo.agar.model

trait GameStateManager:

  def getWorld: World
  def movePlayerDirection(dx: Double, dy: Double): Unit

class DistributedGameStateManager(
    var world: World,
    val player: Player,
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
    val playersEaten = world
      .playersExcludingSelf(player)
      .filter(player => EatingManager.canEatPlayer(playerEatsFood, player))
    val playerEatPlayers = playersEaten.foldLeft(playerEatsFood)((p, other) => p.grow(other))
    world
      .updatePlayer(playerEatPlayers)
      .removePlayers(playersEaten)
      .removeFoods(foodEaten)


  def updatePlayer(player: Player): Unit =
    val w = getWorld
    if (w.players.exists(p => p.id == player.id)) {
      val updatedPlayers = w.players.updated(
        w.players.indexWhere(p => p.id == player.id),
        player
      )
      w.copy(players = updatedPlayers)
    }

  def eatPlayer(player: Player): Unit =
    val w = getWorld
    val updatedPlayers = w.players.filterNot(p => p.id == player.id)
    w.copy(players = updatedPlayers)

  def eatFood(eatenFood: Food, newFood: Food): Unit =
    val w = getWorld
    val updatedFoods = w.foods.filterNot(f => f.id == eatenFood.id) :+ newFood
    w.copy(foods = updatedFoods)