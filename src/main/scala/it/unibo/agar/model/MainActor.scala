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
      val foods = GameInitializer.initialFoods(numFoods, width, height)
      worldOpt = Some(World(width, height, Seq.empty[Player], foods))
      actorsList = List.empty[ActorRef[PlayerActorMessage]]
      this

    case Connect(replyTo) =>
      worldOpt = worldOpt.map(world => {
        val newPlayer = GameInitializer.initialPlayer("p" + (actorsList.length + 1).toString, world.width, world.height)
        val updatedWorld = world.updatePlayer(newPlayer)

        replyTo ! PlayerActor.Boot(context.self, actorsList, updatedWorld, newPlayer)

        val updatedActors = replyTo :: actorsList
        actorsList.foreach(actor =>
//          context.log.info(actor + "will receive: " + updatedActors.filterNot(a => a.equals(actor))
          actor ! SendActors(updatedActors.filterNot(a => a.equals(actor)))
        ) // send to everyone except the new actor
        actorsList = updatedActors

        updatedWorld
      })
      this

    case UpdatePlayer(player) =>
      worldOpt = worldOpt.map(w => w.updatePlayer(player))
      this

    case EatFoods(eatenFoods, newFoods) =>
      worldOpt = worldOpt.map(w => w.removeFoods(eatenFoods).addFoods(newFoods))
      this

    case EatPlayers(players) =>
      worldOpt = worldOpt.map(w => w.removePlayers(players))
      this