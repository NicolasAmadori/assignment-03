package it.unibo.agar.controller

//import akka.actor.typed.{ActorSystem}
//import it.unibo.agar.model.MainActor
//import it.unibo.agar.model.MainActor.Boot
//import it.unibo.agar.startup

//// To run the main actor, use:
//// a) open a sbt shell
//// b) execute, runMain it.unibo.agar.controller.singleMainActor <a seed port>
//// c) when you are done, type exit
//@main def singleMainActor(port: Int) =
//  val width: Int = 1000
//  val height: Int = 1000
//  val numFoods: Int = 100
//  val maxMass: Int = 10000
//  val system: ActorSystem[MainActor.MainActorMessage] = startup(port=port)(MainActor())
//  system ! Boot(width, height, numFoods, maxMass)

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.{ClusterSingleton, ClusterSingletonSettings, SingletonActor}
import it.unibo.agar.model.MainActor
import it.unibo.agar.model.MainActor.Boot
import it.unibo.agar.startupWithRole

@main def singleMainActor(port: Int = 2551) =
  val width: Int = 1000
  val height: Int = 1000
  val numFoods: Int = 100
  val maxMass: Int = 10000
  val system = startupWithRole("main", port) {
    Behaviors.setup { context =>
      val singleton = ClusterSingleton(context.system)

      val mainActorProxy: ActorRef[MainActor.MainActorMessage] = singleton.init(
        SingletonActor(MainActor(), "MainActorSingleton")
          .withSettings(ClusterSingletonSettings(context.system))
      )

      // mando Boot al singleton appena creato
      mainActorProxy ! Boot(width, height, numFoods, maxMass)

      Behaviors.empty // il comportamento principale è delegato al singleton
    }
  }
