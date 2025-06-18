//package it.unibo.agar.model
//
//import akka.actor.typed.scaladsl.Behaviors
//import akka.actor.typed.{ActorRef, Behavior}
//import it.unibo.agar.Message
//import it.unibo.agar.model.MainActor.MainActorMessage
//
//object PlayerActor:
//
//  sealed trait PlayerActorMessage extends Message
//  case class Boot(mainActor: ActorRef[MainActorMessage], actors: List[ActorRef[PlayerActorMessage]], world: World) extends PlayerActorMessage
//  case class SendActors(actors: List[ActorRef[PlayerActorMessage]]) extends PlayerActorMessage
//
//  def boot(): Behavior[PlayerActorMessage] =
//    Behaviors.setup: context =>
//      Behaviors.receiveMessage:
//        case Boot(mainActor, actors, world) =>
//          context.log.info("Received World")
//          receiveGameUpdate(mainActor, actors, world)
//        case _ =>
//          context.log.info("Received anything else while in boot state, ignoring")
//          Behaviors.same
//
//  def receiveGameUpdate(mainActor: ActorRef[MainActorMessage], actors: List[ActorRef[PlayerActorMessage]], world: World): Behavior[PlayerActorMessage] =
//    Behaviors.setup: context =>
//      Behaviors.receiveMessage:
//        case SendActors(actors) =>
//          context.log.info("SendActors received")
//          Behaviors.same
//        case _ =>
//          context.log.info("Received anything else while in receiveGameUpdate state, ignoring")
//          Behaviors.same
//
package it.unibo.agar.model

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import it.unibo.agar.Message
import it.unibo.agar.model.MainActor.MainActorMessage

object PlayerActor:

  sealed trait PlayerActorMessage extends Message
  case class Boot(mainActor: ActorRef[MainActorMessage], actors: List[ActorRef[PlayerActorMessage]], world: World) extends PlayerActorMessage
  case class SendActors(actors: List[ActorRef[PlayerActorMessage]]) extends PlayerActorMessage
  case class UpdatePlayer(player: Player) extends PlayerActorMessage
  case class EatPlayer(player: Player) extends PlayerActorMessage
  case class EatFood(eatenFood: Food, newFood: Food) extends PlayerActorMessage

  def apply(): Behavior[PlayerActorMessage] =
    Behaviors.setup(context => new PlayerActor(context))

class PlayerActor(context: ActorContext[PlayerActor.PlayerActorMessage])
  extends AbstractBehavior[PlayerActor.PlayerActorMessage](context):

  import PlayerActor.*

  private var mainActorOpt: Option[ActorRef[MainActorMessage]] = None
  private var actorsList: List[ActorRef[PlayerActorMessage]] = Nil
  private var gameStateManagerOpt: Option[DistributedGameStateManager] = None
  private var localPlayer: Option[Player] = None

  override def onMessage(msg: PlayerActorMessage): Behavior[PlayerActorMessage] = msg match
    case Boot(mainActor, actors, world) =>
      context.log.info(context.self.toString + ": Boot received")
      mainActorOpt = Some(mainActor)
      actorsList = actors
      localPlayer = Some(GameInitializer.initialPlayer(context.self.toString, world.width, world.height))
      gameStateManagerOpt = Some(DistributedGameStateManager(world, localPlayer.get, context.self))
      this

    case SendActors(newActors) =>
      context.log.info(context.self.toString + ": SendActors received")
      actorsList = newActors
      this

//    case UpdatePlayer(player) =>
//      gameStateManagerOpt.foreach(gsm => gsm.updatePlayer(player))
//      this
//
//    case EatPlayer(player) =>
//      gameStateManagerOpt.foreach(gsm => gsm.eatPlayer(player))
//      this
//
//    case EatFood(eatenFood, newFood) =>
//      gameStateManagerOpt.foreach(gsm => gsm.eatFood(eatenFood, newFood))
//      this

    case _ =>
      context.log.info(context.self.toString + ": Received anything else while in current state, ignoring")
      this
