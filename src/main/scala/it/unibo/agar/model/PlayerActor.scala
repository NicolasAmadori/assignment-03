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
  case class Boot(mainActor: ActorRef[MainActorMessage], actors: List[ActorRef[PlayerActorMessage]], world: World) extends PlayerActorMessage
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
  private var localPlayerOpt: Option[Player] = None

  override def onMessage(msg: PlayerActorMessage): Behavior[PlayerActorMessage] = msg match
    case Boot(mainActor, actors, world) =>
      context.log.info(context.self.toString + ": Boot received")
      mainActorOpt = Some(mainActor)
      actorsList = actors

      val playerId: String = "p" + (actors.length + 1)
      localPlayerOpt = Some(GameInitializer.initialPlayer(playerId, world.width, world.height))

      mainActor ! MainActor.UpdatePlayer(localPlayerOpt.get)
      actors.foreach(_ ! PlayerActor.UpdatePlayer(localPlayerOpt.get))

      gameStateManagerOpt = Some(DistributedGameStateManager(world, localPlayerOpt.get, mainActorOpt.get, actorsList))
      new LocalView(gameStateManagerOpt.get, playerId).open()

      implicit val ec: ExecutionContextExecutor = context.executionContext
      context.system.scheduler.scheduleAtFixedRate(30.millis, 30.millis) {
        () => context.self ! PlayerActor.Tick
      }
      this

    case SendActors(newActors) =>
      context.log.info(context.self.toString + ": SendActors received")
      actorsList = newActors
      this

    case UpdatePlayer(player) =>
      gameStateManagerOpt.foreach(gsm => gsm.world = gameStateManagerOpt.get.world.updatePlayer(player))
      this

    case EatPlayers(players) =>
      gameStateManagerOpt.foreach(gsm => gsm.world = gameStateManagerOpt.get.world.removePlayers(players))
      this

    case EatFoods(eatenFoods, newFoods) =>
      context.log.info("received eatFoods by: " + context.self)
      gameStateManagerOpt.foreach(gsm => gsm.world = gsm.world.removeFoods(eatenFoods).addFoods(newFoods))
      this

    case Tick =>
//      context.log.debug(s"${context.self.path.name}: Tick received")
      gameStateManagerOpt.foreach(_.tick())
      this

    case null =>
      context.log.info(context.self.toString + ": Received anything else while in current state, ignoring")
      this