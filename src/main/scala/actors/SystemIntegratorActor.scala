package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import protocols.{PlaylistProtocols, SongProtocols}

object SystemIntegratorActor {

  // Commands used by the SystemIntegrator for routing the messages to the desired service
  sealed trait Command
  case class RouteToUserService(msg: UserServiceActor.Command, replyTo: ActorRef[String]) extends Command
  case class RouteToSongService(msg: SongProtocols.Command) extends Command
  case class RouteToMusicPlayer(msg: SongProtocols.Command) extends Command
  case class RouteToPlaylistService(msg: PlaylistProtocols.Command) extends Command


  def apply(
             userService: ActorRef[UserServiceActor.Command],
             songService: ActorRef[SongProtocols.Command],
             playlistService: ActorRef[PlaylistProtocols.Command],
             musicPlayer: ActorRef[SongProtocols.Command]
           ): Behavior[Command] = Behaviors.receive { (context, message) =>

    message match {

      // Sends the message to the UserServiceActor
      case RouteToUserService(msg, replyTo) =>
        userService ! msg
        replyTo ! "Message routed to UserService"
        Behaviors.same

      // Sends the message to the SongLibraryActor
      case RouteToSongService(msg) =>
        songService ! msg
        context.log.info(s"Message routed to SongService: $msg")
        Behaviors.same
        // Sends the message to the MusicPlayerActor
        case RouteToMusicPlayer(msg) =>
          musicPlayer ! msg
          context.log.info(s"Message routed to MusicPlayerActor: $msg")
          Behaviors.same

        // Sends the message to the PlaylistServiceActor
        case RouteToPlaylistService(msg) =>
          playlistService ! msg
          context.log.info(s"Message routed to PlaylistServiceActor: $msg")
          Behaviors.same
      }

    }

}
