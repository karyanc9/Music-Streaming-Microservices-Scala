package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import protocols.PlaylistProtocols
import utils.FirebaseUtils
import protocols.PlaylistProtocols.{GetAllPlaylists, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object PlaylistServiceActor {

  def apply(): Behavior[PlaylistProtocols.Command] = Behaviors.setup { context =>

    // Method to format playlist information
    def formatPlaylistInfo(playlist: Map[String, Any]): String = {
      s"Playlist Name: ${playlist.getOrElse("name", "Unknown")}, " +
        s"Playlist ID: ${playlist.getOrElse("id", "Unknown")}, " +
        s"Songs: ${playlist.getOrElse("songs", "No songs in this playlist")}"
    }


    Behaviors.receiveMessage {
      case CreatePlaylist(name) =>
        context.log.info(s"Creating playlist: $name")

        FirebaseUtils.createPlaylist(name).onComplete {
          case Success(playlistId) =>
            context.log.info(s"Playlist created with ID: $playlistId")
          case Failure(exception) =>
            context.log.error(s"Failed to create playlist: ${exception.getMessage}")
        }
        Behaviors.same

      case AddSongToPlaylist(playlistId, songId) =>
        context.log.info(s"Adding song $songId to playlist $playlistId")

        FirebaseUtils.addSongToPlaylist(playlistId, songId).onComplete {
          case Success(_) =>
            context.log.info(s"Song $songId added to playlist $playlistId")
          case Failure(exception) =>
            context.log.error(s"Failed to add song to playlist: ${exception.getMessage}")
        }
        Behaviors.same

      case GetPlaylistSongs(playlistId) => // Handling the new command to get songs in a playlist
        context.log.info(s"Fetching songs for playlist: $playlistId")

        FirebaseUtils.getPlaylists(playlistId).onComplete {
          case Success(result) =>
            result match {
              case Some(playlist) =>
                val songIds = playlist.getOrElse("songs", List[String]()).asInstanceOf[List[String]]
                context.log.info(s"Songs in playlist $playlistId: ${songIds.mkString(", ")}")
              case None =>
                context.log.info(s"Playlist $playlistId not found.")
            }
          case Failure(exception) =>
            context.log.error(s"Failed to fetch songs for playlist: ${exception.getMessage}")
        }
        Behaviors.same

      case RemoveSongFromPlaylist(playlistId, songId) =>
        context.log.info(s"Removing song $songId from playlist $playlistId")

        FirebaseUtils.removeSongFromPlaylist(playlistId, songId).onComplete {
          case Success(_) =>
            context.log.info(s"Song $songId removed from playlist $playlistId")
          case Failure(exception) =>
            context.log.error(s"Failed to remove song from playlist: ${exception.getMessage}")
        }
        Behaviors.same

      case GetPlaylist(playlistId) =>
        context.log.info(s"Fetching playlist: $playlistId")

        FirebaseUtils.getPlaylists(playlistId).onComplete {
          case Success(result) =>
            result match {
              case Some(data) =>
                context.log.info(s"Playlist data: ${formatPlaylistInfo(data)}") // Using formatPlaylistInfo here
              case None =>
                context.log.info("Playlist not found.")
            }
          case Failure(exception) =>
            context.log.error(s"Failed to fetch playlist: ${exception.getMessage}")
        }
        Behaviors.same

      case GetAllPlaylists() =>
        context.log.info("Fetching all playlists")

        FirebaseUtils.getAllPlaylists.onComplete {
          case Success(playlists) =>
            playlists.foreach { playlist =>
              context.log.info(s"Retrieved playlist: ${formatPlaylistInfo(playlist)}")
            }
          case Failure(exception) =>
            context.log.error(s"Failed to fetch playlists: ${exception.getMessage}")
        }
        Behaviors.same

      case RemovePlaylist(playlistId) =>
        context.log.info(s"Removing playlist: $playlistId")

        FirebaseUtils.removePlaylist(playlistId).onComplete {
          case Success(_) =>
            context.log.info(s"Playlist $playlistId removed successfully")
          case Failure(exception) =>
            context.log.error(s"Failed to remove playlist: ${exception.getMessage}")
        }
        Behaviors.same
    }
  }

}
