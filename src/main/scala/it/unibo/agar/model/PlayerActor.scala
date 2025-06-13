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

  def apply(): Behavior[PlayerActorMessage] =
    Behaviors.setup(context => new PlayerActor(context))

class PlayerActor(context: ActorContext[PlayerActor.PlayerActorMessage])
  extends AbstractBehavior[PlayerActor.PlayerActorMessage](context):

  import PlayerActor.*

  private var mainActorOpt: Option[ActorRef[MainActorMessage]] = None
  private var actorsList: List[ActorRef[PlayerActorMessage]] = Nil
  private var worldOpt: Option[World] = None

  override def onMessage(msg: PlayerActorMessage): Behavior[PlayerActorMessage] = msg match
    case Boot(mainActor, actors, world) =>
      context.log.info("Received World")
      mainActorOpt = Some(mainActor)
      actorsList = actors
      worldOpt = Some(world)
      this

    case SendActors(newActors) =>
      context.log.info("SendActors received")
      actorsList = newActors
      this

    case _ =>
      context.log.info("Received anything else while in current state, ignoring")
      this
