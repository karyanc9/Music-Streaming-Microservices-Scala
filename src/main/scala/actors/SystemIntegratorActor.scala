package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import protocols.SongProtocols

object SystemIntegratorActor {
  sealed trait Command
  case class RouteToUserService(msg: UserServiceActor.Command, replyTo: ActorRef[String]) extends Command
  case class RouteToSongService(msg: SongProtocols.Command) extends Command
  case class RouteToMusicPlayer(msg: SongProtocols.Command) extends Command

  def apply(
             userService: ActorRef[UserServiceActor.Command],
             songService: ActorRef[SongProtocols.Command],
             musicPlayer: ActorRef[SongProtocols.Command]
           ): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case RouteToUserService(msg, replyTo) =>
        userService ! msg
        replyTo ! "Message routed to UserService"
        Behaviors.same

      case RouteToSongService(msg) =>
        songService ! msg
        context.log.info(s"Message routed to SongService: $msg")
        Behaviors.same

      case RouteToMusicPlayer(msg) =>
        musicPlayer ! msg
        context.log.info(s"Message routed to MusicPlayerActor: $msg")
        Behaviors.same
    }
  }
}
