package actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import javafx.scene.media.{Media, MediaPlayer}
import protocols.SongProtocols._

object MusicPlayerActor {

  private var currentSongId: Option[String] = None
  private var mediaPlayer: Option[MediaPlayer] = None

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case PlaySong(songId, filePath, replyTo) =>
        // Stop any currently playing song
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


//        try {
//          val media = new Media(new java.io.File(filePath).toURI.toString)
//          mediaPlayer = Some(new MediaPlayer(media))
//          mediaPlayer.foreach(_.play())
//          currentSongId = Some(songId)
//          context.log.info(s"Playing song: $songId from file: $filePath")
//          replyTo ! s"Playing song: $songId"
//        } catch {
//          case e: Exception =>
//            context.log.error(s"Error playing song $songId: ${e.getMessage}")
//            //replyTo ! s"Failed to play song: $songId"
//            if (replyTo != null) replyTo ! s"Failed to play song: $songId"
//        }
//        Behaviors.same

//  def apply(): Behavior[Command] = Behaviors.setup { context =>
//    Behaviors.receiveMessage {
//      case PlaySong(songId, filePath, replyTo) =>
//        playbackState = Playing
//        currentSongId = Some(songId)
//        context.log.info(s"Playing song: $songId from file: $filePath")
//        replyTo ! s"Playing song: $songId"
//        Behaviors.same
//
//      case PauseSong(songId, replyTo) =>
//        if (currentSongId.contains(songId) && playbackState == Playing) {
//          playbackState = Paused
//          context.log.info(s"Paused song: $songId")
//          replyTo ! s"Paused song: $songId"
//        } else {
//          replyTo ! s"Cannot pause song: $songId. It's either not playing or not the current song."
//        }
//        Behaviors.same
//
//      case ResumeSong(songId, replyTo) =>
//        if (currentSongId.contains(songId) && playbackState == Paused) {
//          playbackState = Playing
//          context.log.info(s"Resumed song: $songId")
//          replyTo ! s"Resumed song: $songId"
//        } else {
//          replyTo ! s"Cannot resume song: $songId. It's either not paused or not the current song."
//        }
//        Behaviors.same
//    }
//  }
