//package actors
//
//import akka.actor.typed.{ActorRef, Behavior}
//import akka.actor.typed.scaladsl.Behaviors
//import utils.FirebaseUtils
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.util.{Failure, Success}
//import protocols.SongProtocols._
//
//object SongLibraryActor {
//  // Define commands for the actor
////  sealed trait Command
////  case class AddSong(songId: String, metadata: Map[String, Any]) extends Command
////  case class SearchSong(title: String, replyTo: ActorRef[List[Map[String, Any]]]) extends Command
//
//  def apply(): Behavior[Command] = Behaviors.setup { context =>
//    Behaviors.receiveMessage {
//      case AddSong(songId, metadata) =>
//        context.log.info(s"Adding song: $songId with metadata $metadata")
//
//        // Call `saveSongMetadata` and handle the `Future` result
//        FirebaseUtils.saveSongMetadata(songId, metadata).onComplete {
//          case Success(true) =>
//            context.log.info(s"Song metadata for $songId saved successfully.")
//          case Success(false) =>
//            context.log.error(s"Failed to save song metadata for $songId.")
//          case Failure(exception) =>
//            context.log.error(s"Error saving song metadata for $songId: ${exception.getMessage}")
//        }
//        Behaviors.same
//
//      case SearchSong(title, replyTo) =>
//        context.log.info(s"Searching for song with title: $title")
//
//        // Call `searchSong` and handle the `Future` result
////        FirebaseUtils.searchSong(title).onComplete {
////          case Success(songs) =>
////            context.log.info(s"Replying with search results: $songs")
////            replyTo ! songs
////            context.log.info("Search results sent to reply actor.")
////            if (songs.nonEmpty) context.log.info(s"Search results: $songs")
////            else context.log.info("No songs found with the given title.")
////          case Failure(exception) =>
////            context.log.error(s"Error searching for song: ${exception.getMessage}")
////            replyTo ! Nil
//        // pipe the future result to self
//        val resultFuture = FirebaseUtils.searchSong(title)
//        context.log.info("Initiating pipeToSelf for searchSong Future")
////        resultFuture.onComplete {
////          case Success(songs) =>
////            println(s"searchSong Future completed with: $songs")
////          case Failure(exception) =>
////            println(s"searchSong Future failed with: ${exception.getMessage}")
////        }
//
////        resultFuture.onComplete {
////          case Success(songs) => println(s"searchSong Future completed with: $songs")
////          case Failure(exception) => println(s"searchSong Future failed with: ${exception.getMessage}")
////        }
//
//
//        context.pipeToSelf(resultFuture) {
//          case Success(songs) =>
//            context.log.info(s"PipeToSelf sending WrappedSearchResult with: $songs")
//            WrappedSearchResult(songs, replyTo)
//          case Failure(exception) =>
//            context.log.error(s"PipeToSelf sending WrappedSearchFailure with: ${exception.getMessage}")
//            WrappedSearchFailure(exception, replyTo)
//        }
//        Behaviors.same
//
//        case WrappedSearchResult(songs, replyTo) =>
//          context.log.info(s"Handling WrappedSearchResult: $songs")
//          replyTo ! songs
//          Behaviors.same
//
//        case WrappedSearchFailure(exception, replyTo) =>
//          context.log.error(s"Error searching for song: ${exception.getMessage}")
//          replyTo ! Nil
//          Behaviors.same
//
//        case unexpected =>
//          context.log.error(s"Received unexpected message: $unexpected")
//          Behaviors.same
//    }
//  }
//}


package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import utils.FirebaseUtils
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import protocols.SongProtocols._
import scala.concurrent.Future

object SongLibraryActor {
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    def formatSongInfo(song: Map[String, Any]): String = {
      s"Title: ${song.getOrElse("title", "Unknown")}, " +
        s"Artist: ${song.getOrElse("artist", "Unknown")}, " +
        s"Genre: ${song.getOrElse("genre", "Unknown")}, " +
        s"Duration: ${song.getOrElse("duration", "Unknown")}, " +
        s"FilePath: ${song.getOrElse("filePath", "Unknown")}, " +
        s"ImagePath: ${song.getOrElse("imagePath", "Unknown")} " +
        s"SongId: ${song.getOrElse("id", "Unknown")}"
    }

    Behaviors.receiveMessage {
      case AddSong(songId, metadata) =>
        context.log.info(s"Adding song: $songId with metadata $metadata")
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
        val resultFuture = FirebaseUtils.searchSong(title)
//        resultFuture.onComplete {
//          case Success(songs) => println(s"searchSong Future completed with: $songs")
//          case Failure(exception) => println(s"searchSong Future failed with: ${exception.getMessage}")
//        }
        context.pipeToSelf(resultFuture) {
          case Success(songs) =>
            context.log.info(s"PipeToSelf sending WrappedSearchResult with: $songs")
            WrappedSearchResult(songs, replyTo)
          case Failure(exception) =>
            context.log.error(s"PipeToSelf sending WrappedSearchFailure with: ${exception.getMessage}")
            WrappedSearchFailure(exception, replyTo)
        }
        context.log.info("pipeToSelf initiated for searchSong Future")
        Behaviors.same

      case WrappedSearchResult(songs, replyTo) =>
        if (songs.nonEmpty) {
          context.log.info(s"Number of songs found: ${songs.size}")
          //songs.foreach(song => context.log.info(s"Song Info: $song"))
          songs.foreach(song => context.log.info(s"Song Info: ${formatSongInfo(song)}"))
        } else {
          context.log.info("No songs found.")
        }
        replyTo ! songs
        Behaviors.same

      case WrappedSearchFailure(exception, replyTo) =>
        context.log.error(s"Error searching for song: ${exception.getMessage}")
        replyTo ! Nil
        Behaviors.same
    }
  }
}
