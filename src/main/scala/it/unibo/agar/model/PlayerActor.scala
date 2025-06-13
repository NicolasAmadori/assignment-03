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
