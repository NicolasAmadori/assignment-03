package it.unibo.agar.view

import akka.actor.typed.ActorRef
import it.unibo.agar.model.DistributedGameStateManager
import it.unibo.agar.model.PlayerActor.{PlayerActorMessage, Terminate}

import java.awt.Graphics2D
import javax.swing.SwingUtilities
import scala.swing.*
import scala.swing.event.*

class LocalView(manager: DistributedGameStateManager, playerId: String, playerActorRef: ActorRef[PlayerActorMessage]) extends MainFrame:

  title = s"Agar.io - Local View ($playerId)"
  preferredSize = new Dimension(800, 800)

  override def closeOperation(): Unit = {} // Disabilita la chiusura automatica

  contents = new Panel:
    listenTo(keys, mouse.moves)
    focusable = true
    requestFocusInWindow()

    override def paintComponent(g: Graphics2D): Unit =
      val world = manager.world
      val playerOpt = world.players.find(_.id == playerId)
      val (offsetX, offsetY) = playerOpt
        .map(p => (p.x - size.width / 2.0, p.y - size.height / 2.0))
        .getOrElse((0.0, 0.0))
      AgarViewUtils.drawWorld(g, world, offsetX, offsetY)

    reactions += { case e: event.MouseMoved =>
      val mousePos = e.point
      val playerOpt = manager.world.players.find(_.id == playerId)
      playerOpt.foreach: player =>
        val dx = (mousePos.x - size.width / 2) * 0.01
        val dy = (mousePos.y - size.height / 2) * 0.01
        manager.movePlayerDirection(dx, dy)
      repaint()
    }

  listenTo(this)

  reactions += {
    case WindowClosing(_) =>
      playerActorRef ! Terminate()
  }

  def showView(): Unit =
    SwingUtilities.invokeLater(() => {
      println("showing")
      centerOnScreen()
      open()
      peer.toFront()
      peer.requestFocusInWindow()
    })

  def closeView(): Unit =
    SwingUtilities.invokeLater(() => {
      println("closing")
      close()
      dispose()
    })

  def showMessage(msg: String): Unit =
    SwingUtilities.invokeLater(() => {
      javax.swing.JOptionPane.showMessageDialog(peer, msg, "Game Ended", javax.swing.JOptionPane.INFORMATION_MESSAGE)
    })