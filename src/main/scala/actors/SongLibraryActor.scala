package actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import utils.FirebaseUtils

object SongLibraryActor {
  sealed trait Command
  case class AddSong(songId: String, metadata: Map[String, String]) extends Command
  case class SearchSong(title: String) extends Command

  def apply(): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case AddSong(songId, metadata) =>
        context.log.info(s"Adding song: $songId with metadata")
        FirebaseUtils.saveSongMetadata(songId, metadata)
        Behaviors.same

      case SearchSong(title) =>
        FirebaseUtils.searchSong(title) match {
          case Some(result) => context.log.info(s"Search Result: $result")
          case None         => context.log.info("No song found.")
        }
        Behaviors.same
    }
  }
}
