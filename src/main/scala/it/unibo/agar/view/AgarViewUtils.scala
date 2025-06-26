package it.unibo.agar.view

import it.unibo.agar.model.World

import java.awt.Color
import java.awt.Graphics2D

object AgarViewUtils:

  private val playerBorderColor = Color.black
  private val playerLabelOffsetX = 10
  private val playerLabelOffsetY = 0
  private val playerInnerOffset = 2
  private val playerInnerBorder = 4
  private val playerPalette: Array[Color] =
    Array(Color.blue, Color.orange, Color.cyan, Color.pink, Color.yellow, Color.red, Color.green, Color.lightGray)

  private def playerColor(id: String): Color = id match
    case pid if pid.contains("#") =>
      val idx = pid.split("#").lift(1).flatMap(_.toIntOption).getOrElse(0)
      playerPalette(idx % playerPalette.length)
    case _ => Color.gray

  def drawWorld(
                 g: Graphics2D,
                 world: World,
                 offsetX: Double = 0,
                 offsetY: Double = 0
               ): Unit =
    def toScreenCenter(x: Double, y: Double, radius: Int): (Int, Int) =
      ((x - offsetX - radius).toInt, (y - offsetY - radius).toInt)

    def toScreenLabel(x: Double, y: Double): (Int, Int) =
      ((x - offsetX - playerLabelOffsetX).toInt, (y - offsetY - playerLabelOffsetY).toInt)

    g.setColor(Color.black)
    val borderX = (0 - offsetX).toInt
    val borderY = (0 - offsetY).toInt
    g.drawRect(borderX, borderY, world.width, world.height)

    // Draw foods
    g.setColor(Color.green)
    world.foods.foreach: food =>
      val radius = food.radius.toInt
      val diameter = radius * 2
      val (foodX, foodY) = toScreenCenter(food.x, food.y, radius)
      g.fillOval(foodX, foodY, diameter, diameter)

    // Draw players
    world.players.foreach: player =>
      val radius = player.radius.toInt
      val diameter = radius * 2
      val (borderX, borderY) = toScreenCenter(player.x, player.y, radius)
      g.setColor(playerBorderColor)
      g.drawOval(borderX, borderY, diameter, diameter)
      g.setColor(playerColor(player.id))
      val (innerX, innerY) = toScreenCenter(player.x, player.y, radius - playerInnerOffset)
      g.fillOval(innerX, innerY, diameter - playerInnerBorder, diameter - playerInnerBorder)
      g.setColor(playerBorderColor)
      val (labelX, labelY) = toScreenLabel(player.x, player.y)
      g.drawString(player.id + " (" + player.mass.toInt + ")", labelX, labelY)

    val bounds = g.getClipBounds()
    val leaderboardX = 30 // Margine dal bordo destro
    val leaderboardY = 30 // Margine dal bordo superiore
    val lineHeight = 15 // Spazio verticale tra le righe

    g.setColor(Color.BLACK)
    g.drawString("Leaderboard", leaderboardX, leaderboardY)

    val topPlayers = world.players.sortBy(player => (-player.mass, player.id)).take(3)

    topPlayers.zipWithIndex.foreach { case (player, index) =>
      val rank = index + 1
      val playerInfo = s"$rank. ${player.id} (${player.mass.toInt})"
      val currentY = leaderboardY + (rank * lineHeight)
      g.drawString(playerInfo, leaderboardX, currentY)
    }