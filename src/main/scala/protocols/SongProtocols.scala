package protocols

import akka.actor.typed.ActorRef

object SongProtocols {
  sealed trait Command
  // Song Library Commands
  case class AddSong(songId: String, metadata: Map[String, Any]) extends Command
  case class SearchSong(title: String, replyTo: akka.actor.typed.ActorRef[List[Map[String, Any]]]) extends Command
  case class WrappedSearchResult(songs: List[Map[String, Any]], replyTo: ActorRef[List[Map[String, Any]]]) extends Command
  case class WrappedSearchFailure(exception: Throwable, replyTo: ActorRef[List[Map[String, Any]]]) extends Command

  // Playback Commands
  case class PlaySong(songId: String, filePath: String, replyTo: ActorRef[String]) extends Command
  case class PauseSong(songId: String, replyTo: ActorRef[String]) extends Command
  case class ResumeSong(songId: String, replyTo: ActorRef[String]) extends Command
}
