package it.unibo.agar.model

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import it.unibo.agar.Message
import it.unibo.agar.model.PlayerActor.{SendActors, PlayerActorMessage}

object MainActor:

  sealed trait MainActorMessage extends Message
  case class Boot(width: Int, height: Int, numFoods: Int) extends MainActorMessage
  case class Connect(replyTo: ActorRef[PlayerActorMessage]) extends MainActorMessage
  case class UpdatePlayer(player: Player) extends MainActorMessage
  case class EatPlayers(eatenPlayers: Seq[Player]) extends MainActorMessage
  case class EatFoods(eatenFoods: Seq[Food], newFoods: Seq[Food]) extends MainActorMessage

  // Factory method per l'actor
  def apply(): Behavior[MainActorMessage] =
    Behaviors.setup(context => new MainActor(context))

class MainActor(context: ActorContext[MainActor.MainActorMessage])
  extends AbstractBehavior[MainActor.MainActorMessage](context):

  import MainActor.*

  private var actorsList: List[ActorRef[PlayerActorMessage]] = Nil
  private var worldOpt: Option[World] = None

  override def onMessage(msg: MainActorMessage): Behavior[MainActorMessage] = msg match
    case Boot(width, height, numFoods) =>
      context.log.info("Received Boot: " + context.self)

      val foods = GameInitializer.initialFoods(numFoods, width, height)
      worldOpt = Some(World(width, height, Seq.empty[Player], foods))
      actorsList = List.empty[ActorRef[PlayerActorMessage]]
      context.log.info("Boot complete, transitioning to connect behavior.")
      this

    case Connect(replyTo) =>
      worldOpt.foreach(w => {
        context.log.info("Received Connect, sending boot to: " + replyTo)
        replyTo ! PlayerActor.Boot(context.self, actorsList, w)
        val updatedActors = replyTo :: actorsList
        actorsList.foreach(_ ! SendActors(updatedActors)) // send to everyone except the new actor
        actorsList = updatedActors
        context.log.info(w.players.toString())
      })
      this

    case UpdatePlayer(player) =>
      worldOpt.map(w => w.updatePlayer(player))
      this

    case EatFoods(eatenFoods, newFoods) =>
      worldOpt.map(w => w.removeFoods(eatenFoods).addFoods(newFoods))
      this

    case EatPlayers(players) =>
      worldOpt.map(w => w.removePlayers(players))
      this