package it.unibo.agar.model

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import it.unibo.agar.Message
import it.unibo.agar.model.PlayerActor.{SendActors, PlayerActorMessage}

object MainActor:

  sealed trait MainActorMessage extends Message
  case class Boot(width: Int, height: Int, numFoods: Int, maxMass: Int) extends MainActorMessage
  case class Connect(playerName: String, replyTo: ActorRef[PlayerActorMessage]) extends MainActorMessage
  case class UpdatePlayer(player: Player) extends MainActorMessage
  case class EatPlayers(eatenPlayers: Seq[Player]) extends MainActorMessage
  case class EatFoods(eatenFoods: Seq[Food], newFoods: Seq[Food]) extends MainActorMessage
  case class Disconnect(replyTo: ActorRef[PlayerActorMessage], playerId: String) extends MainActorMessage

  // Factory method per l'actor
  def apply(): Behavior[MainActorMessage] =
    Behaviors.setup(context => new MainActor(context))

class MainActor(context: ActorContext[MainActor.MainActorMessage])
  extends AbstractBehavior[MainActor.MainActorMessage](context):

  import MainActor.*

  private var actorsList: List[ActorRef[PlayerActorMessage]] = Nil
  private var worldOpt: Option[World] = None
  private var playerCounter: Int = 0

  override def onMessage(msg: MainActorMessage): Behavior[MainActorMessage] = msg match
    case Boot(width, height, numFoods, maxMass) =>
      val foods = GameInitializer.initialFoods(numFoods, width, height)
      worldOpt = Some(World(width, height, maxMass, Seq.empty[Player], foods))
      actorsList = List.empty[ActorRef[PlayerActorMessage]]
      this

    case Connect(playerName, replyTo) =>
      worldOpt = worldOpt.map(world => {
        val newPlayer = GameInitializer.initialPlayer(playerName + "#" + playerCounter, world.width, world.height)
        playerCounter+=1
        val updatedWorld = world.updatePlayer(newPlayer)

        replyTo ! PlayerActor.Boot(context.self, actorsList, updatedWorld, newPlayer)

        val updatedActors = replyTo :: actorsList
        actorsList.foreach(actor =>
          actor ! SendActors(updatedActors.filterNot(a => a.equals(actor)))
        )
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

    case Disconnect(replyTo, playerId) =>
      worldOpt = worldOpt.map { w =>
        w.playerById(playerId) match {
          case Some(player) => w.removePlayers(Seq(player))
          case None => w
        }
      }
      actorsList = actorsList.filterNot(actor => actor.equals(replyTo))
      actorsList.foreach(actor => actor ! SendActors(actorsList.filterNot(a => a.equals(actor))))
      this