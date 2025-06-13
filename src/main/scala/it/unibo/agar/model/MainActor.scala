package it.unibo.agar.model

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import it.unibo.agar.Message
import it.unibo.agar.model.PlayerActor.{SendActors, PlayerActorMessage}

object MainActor:

  sealed trait MainActorMessage extends Message
  case class Boot(width: Int, height: Int, numFoods: Int) extends MainActorMessage
  case class Connect(replyTo: ActorRef[PlayerActorMessage]) extends MainActorMessage

  final case class State(
                          mainActor: ActorRef[MainActorMessage],
                          actors: List[ActorRef[PlayerActorMessage]],
                          world: World
                        )

  // Factory method per l'actor
  def apply(): Behavior[MainActorMessage] =
    Behaviors.setup(context => new MainActor(context))

class MainActor(context: ActorContext[MainActor.MainActorMessage])
  extends AbstractBehavior[MainActor.MainActorMessage](context):

  import MainActor.*

  private var stateOpt: Option[State] = None

  override def onMessage(msg: MainActorMessage): Behavior[MainActorMessage] = msg match
    case Boot(width, height, numFoods) =>
      context.log.info("Received Boot: " + context.self)

      val foods = GameInitializer.initialFoods(numFoods, width, height)
      val actors = List.empty[ActorRef[PlayerActorMessage]]
      val mainActorRef = context.self

      val world = World(width, height, Seq.empty[Player], foods)
      val newState = State(mainActorRef, actors, world)

      stateOpt = Some(newState)

      context.log.info("Boot complete, transitioning to connect behavior.")
      this

    case Connect(replyTo) =>
      stateOpt match
        case Some(state) =>
          context.log.info("Received Connect, sending world from: " + context.self)
          replyTo ! PlayerActor.Boot(state.mainActor, state.actors, state.world)

          val updatedActors = replyTo :: state.actors
          updatedActors.foreach(_ ! SendActors(updatedActors))

          val newState = state.copy(actors = updatedActors)
          stateOpt = Some(newState)
          this

        case None =>
          context.log.info("Received Connect before Boot, ignoring")
          this
