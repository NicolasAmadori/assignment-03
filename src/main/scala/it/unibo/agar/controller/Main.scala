package it.unibo.agar.controller

import java.time.Duration
import akka.actor.typed.{ActorSystem, ActorRef}
import it.unibo.agar.model.*
import it.unibo.agar.model.MainActor.{Boot, Connect}
import it.unibo.agar.model.PlayerActor.PlayerActorMessage

//import it.unibo.agar.view.GlobalView
import it.unibo.agar.view.LocalView

import java.awt.Window
import java.util.Timer
import java.util.TimerTask
import scala.swing.*
import scala.swing.Swing.onEDT

object Main extends SimpleSwingApplication:

  private val width = 1000
  private val height = 1000
  private val numFoods = 100

//  private val timer = new Timer()
//  private val task: TimerTask = new TimerTask:
//    override def run(): Unit =
//      AIMovement.moveAI("p1", manager)
//      manager.tick()
//      onEDT(Window.getWindows.foreach(_.repaint()))
//  timer.scheduleAtFixedRate(task, 0, 30) // every 30ms

  private val system: ActorSystem[MainActor.MainActorMessage] =
    ActorSystem(MainActor(), "AgarSystem")

  system ! Boot(width, height, numFoods)

  private val player1: ActorRef[PlayerActorMessage] = system.systemActorOf(PlayerActor(), "player1")
  private val player2: ActorRef[PlayerActorMessage] = system.systemActorOf(PlayerActor(), "player2")
//  private val player3: ActorRef[PlayerActorMessage] = system.systemActorOf(PlayerActor(), "player3")
//  private val player4: ActorRef[PlayerActorMessage] = system.systemActorOf(PlayerActor(), "player4")
//  private val player5: ActorRef[PlayerActorMessage] = system.systemActorOf(PlayerActor(), "player5")
  
  system.scheduler.scheduleOnce(
    Duration.ofMillis(500),
    () => {
      system ! Connect(player1)
      system ! Connect(player2)
//      system ! Connect(player3)
//      system ! Connect(player4)
//      system ! Connect(player5)
    },
    system.executionContext
  )

  println("Avviato sistema di test attori")

  override def top: Frame =
    // Open both views at startup
//    new GlobalView(manager).open()
//    new LocalView(manager, "p1").open()
//    new LocalView(manager, "p2").open()
    // No launcher window, just return an empty frame (or null if allowed)
    new Frame {
      visible = false
    }