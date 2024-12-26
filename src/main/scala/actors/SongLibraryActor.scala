package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import utils.FirebaseUtils
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object SongLibraryActor {
  // Define commands for the actor
  sealed trait Command
  case class AddSong(songId: String, metadata: Map[String, Any]) extends Command
  case class SearchSong(title: String, replyTo: ActorRef[List[Map[String, Any]]]) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case AddSong(songId, metadata) =>
        context.log.info(s"Adding song: $songId with metadata $metadata")

        // Call `saveSongMetadata` and handle the `Future` result
        FirebaseUtils.saveSongMetadata(songId, metadata).onComplete {
          case Success(true) =>
            context.log.info(s"Song metadata for $songId saved successfully.")
          case Success(false) =>
            context.log.error(s"Failed to save song metadata for $songId.")
          case Failure(exception) =>
            context.log.error(s"Error saving song metadata for $songId: ${exception.getMessage}")
        }
        Behaviors.same

      case SearchSong(title, replyTo) =>
        context.log.info(s"Searching for song with title: $title")

        // Call `searchSong` and handle the `Future` result
        FirebaseUtils.searchSong(title).onComplete {
          case Success(songs) =>
            replyTo ! songs
            if (songs.nonEmpty) context.log.info(s"Search results: $songs")
            else context.log.info("No songs found with the given title.")
          case Failure(exception) =>
            context.log.error(s"Error searching for song: ${exception.getMessage}")
            replyTo ! Nil
        }
        Behaviors.same
    }
  }
}
