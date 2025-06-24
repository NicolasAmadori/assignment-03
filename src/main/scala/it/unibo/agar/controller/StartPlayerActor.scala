package it.unibo.agar.controller

// To run a player actor, use:
// a) open a sbt shell
// b) execute, runMain it.unibo.agar.controller.singlePlayerActor <a seed port>
// c) when you are done, type exit
//@main def singlePlayerActor(port: Int) =
//  val system: ActorSystem[Player.MainActorMessage] = startup(port=port)(MainActor())
//  system ! Boot(width, height, numFoods, maxMass)
//
//  startup(port=port)(MainActor())

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.{ClusterSingleton, ClusterSingletonSettings, SingletonActor}
import it.unibo.agar.model.{MainActor, PlayerActor}
import it.unibo.agar.model.MainActor.MainActorMessage
import it.unibo.agar.model.PlayerActor.PlayerActorMessage
import it.unibo.agar.startupWithRole

@main def singlePlayerActor(port: Int, playerName: String) =
  val system: ActorSystem[PlayerActorMessage] = startupWithRole("player", port) {
    Behaviors.setup { context =>
      // Otteniamo il proxy singleton per MainActor
      val singleton = ClusterSingleton(context.system)
      val mainActorProxy: ActorRef[MainActorMessage] =
        singleton.init(
          SingletonActor(MainActor(), "MainActorSingleton")
            .withSettings(ClusterSingletonSettings(context.system).withRole("main"))
        )

      // Creiamo il player actor
      val playerActor = context.spawn(PlayerActor(), playerName)

      // Connettiamo player a mainActor
      mainActorProxy ! MainActor.Connect(playerName, playerActor)

      // Avviamo playerActor inviando Boot (con dati base, potresti anche recuperarli da MainActor)
      // Qui potresti anche mandare un messaggio per inizializzare il player localmente,
      // ma in realtà la logica di Boot la fa MainActor che risponde al player con i dati del mondo.

      Behaviors.empty // solo attiviamo playerActor come child, la logica è dentro PlayerActor
    }
  }

  println(s"Player actor $playerName started on port $port")
