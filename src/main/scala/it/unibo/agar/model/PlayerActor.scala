package it.unibo.agar.model

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import it.unibo.agar.Message
import it.unibo.agar.model.MainActor.MainActorMessage
import it.unibo.agar.view.LocalView

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

object PlayerActor:

  sealed trait PlayerActorMessage extends Message
  case class Boot(mainActor: ActorRef[MainActorMessage], actors: List[ActorRef[PlayerActorMessage]], world: World, player: Player) extends PlayerActorMessage
  case class SendActors(actors: List[ActorRef[PlayerActorMessage]]) extends PlayerActorMessage
  case class UpdatePlayer(player: Player) extends PlayerActorMessage
  case class EatPlayers(eatenPlayers: Seq[Player]) extends PlayerActorMessage
  case class EatFoods(eatenFoods: Seq[Food], newFoods: Seq[Food]) extends PlayerActorMessage
  case object Tick extends PlayerActorMessage

  def apply(): Behavior[PlayerActorMessage] =
    Behaviors.setup(context => new PlayerActor(context))

class PlayerActor(context: ActorContext[PlayerActor.PlayerActorMessage])
  extends AbstractBehavior[PlayerActor.PlayerActorMessage](context):

  import PlayerActor.*

  private var mainActorOpt: Option[ActorRef[MainActorMessage]] = None
  private var actorsList: List[ActorRef[PlayerActorMessage]] = Nil
  private var gameStateManagerOpt: Option[DistributedGameStateManager] = None
  private var localPlayerIdOpt: Option[String] = None
  private var localViewOpt: Option[LocalView] = None

  override def onMessage(msg: PlayerActorMessage): Behavior[PlayerActorMessage] = msg match
    case Boot(mainActor, actors, world, player) =>
      mainActorOpt = Some(mainActor)
      actorsList = actors
      localPlayerIdOpt = Some(player.id)

      mainActor ! MainActor.UpdatePlayer(player)
      actors.foreach(_ ! PlayerActor.UpdatePlayer(player))

      gameStateManagerOpt = Some(DistributedGameStateManager(world, player, mainActorOpt.get, actorsList))

      localViewOpt = Some(new LocalView(gameStateManagerOpt.get, player.id))
      localViewOpt.get.open()

      implicit val ec: ExecutionContextExecutor = context.executionContext
      context.system.scheduler.scheduleAtFixedRate(30.millis, 30.millis) {
        () => context.self ! PlayerActor.Tick
      }
      this

    case SendActors(newActors) =>
      gameStateManagerOpt.foreach(gsm => gsm.actors = newActors)
      this

    case UpdatePlayer(player) =>
      gameStateManagerOpt.foreach(gsm => gsm.world = gameStateManagerOpt.get.world.updatePlayer(player))
      this

    case EatPlayers(players) =>
      if (players.map(_.id).contains(localPlayerIdOpt.get)) {
        localViewOpt.foreach(_.close())
        Behaviors.stopped
      } else {
        gameStateManagerOpt.foreach(gsm => gsm.world = gameStateManagerOpt.get.world.removePlayers(players))
        Behaviors.same
      }

    case EatFoods(eatenFoods, newFoods) =>
      gameStateManagerOpt.foreach(gsm => gsm.world = gsm.world.removeFoods(eatenFoods).addFoods(newFoods))
      this

    case Tick =>
      gameStateManagerOpt.foreach(_.tick())
      localViewOpt.foreach(_.repaint())
      this

    case null =>
      context.log.info(context.self.toString + ": Received anything else while in current state, ignoring")
      this