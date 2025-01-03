package main

import PlaylistManagerUI.systemIntegrator
import actors.{MusicPlayerActor, PlaylistServiceActor, SystemIntegratorActor}
import scalafx.application.Platform
import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, GridPane, VBox}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.control.{Alert, Button, Label, TextInputDialog}
import scalafx.scene.control.Alert.AlertType
import scalafx.geometry.{Insets, Pos}
import utils.FirebaseUtils
import models.SongInfo
import akka.actor.typed.{ActorRef, ActorSystem}
import main.MusicPlayerUI
import protocols.{PlaylistProtocols, SongProtocols}

import java.io.FileInputStream
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object PlaylistSongsUI {

  // Current stage reference
  var stageRef: Option[scalafx.stage.Stage] = None

  // Reference to the PlaylistServiceActor
  val playlistServiceActor: ActorSystem[PlaylistProtocols.Command] = ActorSystem(PlaylistServiceActor(), "PlaylistServiceActor")
  val musicPlayerActor: ActorSystem[SongProtocols.Command] = ActorSystem(MusicPlayerActor(), "MusicPlayerActor")

  // Initialize systemIntegrator with ActorRef properly
  implicit val systemIntegrator: ActorRef[SystemIntegratorActor.Command] =
    playlistServiceActor.systemActorOf(SystemIntegratorActor(null, null, playlistServiceActor, musicPlayerActor), "SystemIntegratorActor")

  def fetchPlaylistSongs(playlistId: String): Unit = {
    println(s"Fetching songs for playlist with ID: $playlistId")

    FirebaseUtils.getPlaylistSongs(playlistId).onComplete {
      case Success(songDetailsList: List[Map[String, Any]]) =>
        val songDataList: List[SongInfo] = songDetailsList
          // Filter out null songs
          .filter(_ != null) // Remove null entries from the list
          .flatMap { songData =>
            // Ensure song data is valid and extract necessary fields
            val title = songData.getOrElse("title", "").toString
            val id = songData.getOrElse("songId", "").toString // `songId` is used as the identifier now
            val imagePath = songData.getOrElse("imagePath", "").toString
            val filePath = songData.getOrElse("filePath", "").toString

            // Only include songs that have valid title, id, and filePath
            if (title.nonEmpty && id.nonEmpty && filePath.nonEmpty) {
              println(s"Mapping song - Title: $title, ImagePath: $imagePath, FilePath: $filePath")
              Some(SongInfo(title, id, imagePath, filePath)) // Valid song
            } else {
              println(s"Skipping invalid song - Title: $title, ImagePath: $imagePath, FilePath: $filePath")
              None // Invalid song, skip it
            }
          }

        // Update UI on the JavaFX thread
        Platform.runLater {
          updateUI(songDataList, playlistId)
        }

      case Failure(exception) =>
        println(s"Failed to fetch playlist songs: ${exception.getMessage}")
    }
  }




  private def deleteSongFromPlaylist(playlistId: String, songId: String, songTitle: String): Unit = {
    // Ensure `playlistServiceActor` is properly passed
    systemIntegrator ! SystemIntegratorActor.RouteToPlaylistService(PlaylistProtocols.RemoveSongFromPlaylist(playlistId, songId))

    // Notify the user
    new Alert(AlertType.Information) {
      title = "Success"
      headerText = "Song Deleted"
      contentText = s" '$songTitle' has been deleted from the playlist."
    }.showAndWait()

    // Re-fetch the playlist songs to update UI
    fetchPlaylistSongs(playlistId)
  }

  private def updateUI(songs: List[SongInfo], playlistId: String): Unit = {
    val gridPane = new GridPane {
      hgap = 20
      vgap = 20
      padding = Insets(20)
      style = "-fx-background-color: #2A2A2A;" // Dark background color for the grid
    }

    val contentPane = new BorderPane
    contentPane.style = "-fx-background-color: #2A2A2A;" // Dark background color for the content area

    if (songs.isEmpty) {
      contentPane.center = new Label("No songs in this playlist.") {
        style = "-fx-font-size: 16px; -fx-text-fill: #B0B0B0;" // Lighter gray color for the text
      }
    } else {
      songs.zipWithIndex.foreach { case (song, index) =>
        val row = index / 3
        val col = index % 3
        gridPane.add(createSongBox(song, playlistId), col, row)
      }
      contentPane.center = gridPane
    }

    // Add "Add Song" button
    val addButton = new Button("Add Song") {
      style =
        """
          | -fx-background-color: #1DB954;
          | -fx-text-fill: white;
          | -fx-font-size: 16px;
          | -fx-font-weight: bold;
          | -fx-border-radius: 30;
          | -fx-background-radius: 30;
          | -fx-padding: 10 30;
          | -fx-effect: dropshadow(three-pass-box, #000000, 5, 0, 0, 5);
             """.stripMargin
      onAction = _ => {
        println(s"Adding song to playlist with ID: $playlistId")
        showAddSongDialog(playlistId)
      }
    }

    contentPane.bottom = new VBox {
      alignment = Pos.Center
      spacing = 20
      padding = Insets(20)
      children = Seq(addButton)
    }

    stageRef.foreach { stage =>
      stage.scene = new Scene(600, 400) {
        root = contentPane
      }
    }
  }

  private def createSongBox(song: SongInfo, playlistId: String): VBox = {
    val imageView = new ImageView(new Image(new FileInputStream(song.imagePath))) {
      fitWidth = 100
      fitHeight = 100
      preserveRatio = true
    }

    val deleteButton = new Button("Delete") {
      style =
        """
          | -fx-background-color: #FF5555;
          | -fx-text-fill: white;
          | -fx-font-size: 14px;
          | -fx-border-radius: 30;
          | -fx-background-radius: 30;
          | -fx-padding: 8 20;
          | -fx-effect: dropshadow(three-pass-box, #000000, 5, 0, 0, 3);
             """.stripMargin
      onAction = _ => {
        println(s"Deleting song with id: ${song.id} from playlist: ${playlistId} ")
        deleteSongFromPlaylist(playlistId, song.id, song.title)
      }
    }

    val songBox = new VBox {
      spacing = 15
      alignment = Pos.Center
      padding = Insets(15)
      style =
        """
          | -fx-background-color: #333333;
          | -fx-border-radius: 20;
          | -fx-background-radius: 20;
          | -fx-padding: 15;
          | -fx-effect: dropshadow(three-pass-box, #000000, 10, 0, 0, 3);
             """.stripMargin
      children = Seq(imageView, new Label(song.title) {
        style =
          """
            | -fx-font-size: 16px;
            | -fx-font-weight: bold;
            | -fx-text-fill: white;
               """.stripMargin
      }, deleteButton)
    }

    // Navigate to Music Player on click
    songBox.onMouseClicked = _ => {
      println(s"Opening music player for song: ${song.title}")
      MusicPlayerUI.showWithSongInfo(song)(systemIntegrator) // Pass systemIntegrator directly here
    }

    songBox
  }


  private def showAddSongDialog(playlistId: String): Unit = {
    val dialog = new TextInputDialog() {
      title = "Add Song"
      headerText = "Add a new song to the playlist"
      contentText = "Enter song ID:"
    }

    val result = dialog.showAndWait()

    result match {
      case Some(songId) if songId.nonEmpty =>
        // Attempt to add the song to the playlist
        println(s"Attempting to add song with ID: $songId to playlist $playlistId")
        systemIntegrator ! SystemIntegratorActor.RouteToPlaylistService(PlaylistProtocols.AddSongToPlaylist(playlistId, songId))

        // Run a background task to introduce a delay before refreshing UI
        Future {
          // Introduce a delay for firebase refresh
          Thread.sleep(500)

          // After the delay, refresh the UI on the JavaFX thread
          Platform.runLater {
            println("Refreshing UI after song addition...")
            fetchPlaylistSongs(playlistId) // Refresh songs
          }
        }

      case Some(_) =>
        // If the song ID is empty, just refresh the UI without alert
        println("Empty song ID entered, refreshing playlist...")
        Platform.runLater {
          fetchPlaylistSongs(playlistId) // Refresh songs
        }

      case None =>
        // User canceled the dialog, still refresh the UI
        println("User canceled, refreshing playlist...")
        Platform.runLater {
          fetchPlaylistSongs(playlistId) // Refresh songs
        }
    }
  }

  // Show the PlaylistSongsUI with fixed window size
  def show(playlistId: String, serviceActor: ActorRef[PlaylistProtocols.Command], callback: () => Unit): Unit = {
    val stage = new scalafx.stage.Stage {
      title = "Playlist Songs"
      width = 600 // Fixed width
      height = 400 // Fixed height
      resizable = false // Prevent resizing
      scene = new Scene(600, 400) {
        content = new Label("Loading songs...")
      }
    }
    stageRef = Some(stage)
    fetchPlaylistSongs(playlistId)

    stage.setOnCloseRequest(_ => callback())

    stage.show()
  }
}
