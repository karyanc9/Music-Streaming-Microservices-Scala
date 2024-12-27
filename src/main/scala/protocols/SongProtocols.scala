package protocols

import akka.actor.typed.ActorRef

object SongProtocols {
  sealed trait Command
  case class AddSong(songId: String, metadata: Map[String, Any]) extends Command
  case class SearchSong(title: String, replyTo: akka.actor.typed.ActorRef[List[Map[String, Any]]]) extends Command
  case class WrappedSearchResult(songs: List[Map[String, Any]], replyTo: ActorRef[List[Map[String, Any]]]) extends Command
  case class WrappedSearchFailure(exception: Throwable, replyTo: ActorRef[List[Map[String, Any]]]) extends Command

}
