package actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import utils.FirebaseUtils


object PlaylistServiceActor {
  sealed trait Command
  case class CreatePlaylist(playlistId: String, data: Map[String, Any]) extends Command
  case class GetPlaylist(playlistId: String) extends Command

  def apply(): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case CreatePlaylist(playlistId, data) =>
        FirebaseUtils.savePlaylist(playlistId, data)
        context.log.info(s"Playlist $playlistId created.")
        Behaviors.same

      case GetPlaylist(playlistId) =>
        FirebaseUtils.getPlaylist(playlistId) match {
          case Some(result) => context.log.info(s"Playlist data: $result")
          case None         => context.log.info("Playlist not found.")
        }
        Behaviors.same
    }
  }
}
