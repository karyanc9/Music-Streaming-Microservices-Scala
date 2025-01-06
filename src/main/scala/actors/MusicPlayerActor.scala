package actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import javafx.scene.media.{Media, MediaPlayer}
import protocols.SongProtocols._

object MusicPlayerActor {

  // variables used by the MusicPlayerActor
  private var currentSongId: Option[String] = None
  private var mediaPlayer: Option[MediaPlayer] = None

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case PlaySong(songId, filePath, replyTo) =>
        // Stop any currently playing song and play the new song instead
        if (mediaPlayer.isDefined) {
          context.log.info(s"Stopping currently playing song: ${currentSongId.getOrElse("Unknown")}")
          mediaPlayer.foreach(_.stop())
          mediaPlayer = None
          currentSongId = None
        }

        try {
          val media = new Media(new java.io.File(filePath).toURI.toString)
          mediaPlayer = Some(new MediaPlayer(media))
          mediaPlayer.foreach(_.play())
          currentSongId = Some(songId)
          context.log.info(s"Playing song: $songId from $filePath")
          if (replyTo != null) replyTo ! s"Playing song: $songId"
        } catch {
          case e: Exception =>
            context.log.error(s"Failed to play song: $songId. Error: ${e.getMessage}")
            if (replyTo != null) replyTo ! s"Failed to play song: $songId"
        }
        Behaviors.same

      // pause a song thats currently playing
      case PauseSong(songId, replyTo) =>
        if (currentSongId.contains(songId) && mediaPlayer.isDefined) {
          mediaPlayer.foreach(_.pause())
          context.log.info(s"Paused song: $songId")
          if (replyTo != null) replyTo ! s"Paused song: $songId"
        } else {
          context.log.error(s"Cannot pause song: $songId. It is not currently playing.")
          if (replyTo != null) replyTo ! s"Cannot pause song: $songId"
        }
        Behaviors.same


    }
  }
}

