package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import utils.FirebaseUtils
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

object PlaylistServiceActor {
  sealed trait Command
  case class GetPlaylist(playlistId: String, replyTo: ActorRef[Option[Map[String, Any]]]) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case GetPlaylist(playlistId, replyTo) =>
        context.log.info(s"Fetching playlist: $playlistId")

        // Fetch the playlist from FirebaseUtils
        FirebaseUtils.getPlaylist(playlistId).onComplete {
          case Success(result) =>
            replyTo ! result // Send the Option[Map[String, Any]] back to the caller
            result match {
              case Some(data) => context.log.info(s"Playlist data: $data")
              case None       => context.log.info("Playlist not found.")
            }

          case Failure(exception) =>
            context.log.error(s"Failed to fetch playlist: ${exception.getMessage}")
            replyTo ! None
        }

        Behaviors.same
    }
  }
}
