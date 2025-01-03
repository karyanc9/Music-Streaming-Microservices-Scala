package main

import akka.actor.typed.ActorSystem
import actors.{MusicPlayerActor, PlaylistServiceActor, SongLibraryActor, SystemIntegratorActor}
import akka.actor.typed.scaladsl.Behaviors
import protocols.{PlaylistProtocols, SongProtocols}
import protocols.SongProtocols.SearchSong
import scalafx.Includes.jfxSceneProperty2sfx
import scalafx.application.{Platform}
import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, GridPane, HBox, StackPane, VBox}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.control.{Button, Label, ProgressIndicator, ScrollPane, TextField}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.effect.DropShadow
import scalafx.scene.paint.Color
import scalafx.scene.text.Font
import utils.FirebaseUtils

import java.io.FileInputStream
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object SongLibraryUI {

  case class SongData(title: String, imagePath: String, filePath: String)

  // Initialize the actors
  val songLibrary: ActorSystem[protocols.SongProtocols.Command] = ActorSystem(SongLibraryActor(), "SongLibraryActor")
  val musicPlayerActor: ActorSystem[SongProtocols.Command] = ActorSystem(MusicPlayerActor(), "MusicPlayerActor")
  val playlistServiceActor: ActorSystem[PlaylistProtocols.Command] = ActorSystem(PlaylistServiceActor(), "PlaylistServiceActor")
  implicit val systemIntegrator: ActorSystem[SystemIntegratorActor.Command] = ActorSystem(
    SystemIntegratorActor(null, songLibrary, playlistServiceActor, musicPlayerActor),
    "SystemIntegratorActor"
  )

  // UI Components
  val searchField = new TextField {
    promptText = "Search for a song..."
    prefWidth = 300
    style =
      s"""
        -fx-background-color: #333333;
        -fx-text-fill: #FFFFFF;
        -fx-background-radius: 15;
        -fx-padding: 8;
      """
    font = Font("Arial", 14)
  }

  var searchTimer: Option[java.util.Timer] = None

  // Debounce logic for the search field
  def debounceSearch(): Unit = {
    searchTimer.foreach(_.cancel())

    searchTimer = Some(new java.util.Timer())
    searchTimer.get.schedule(new java.util.TimerTask {
      override def run(): Unit = {
        handleSearch() // Call the handleSearch() method after the delay
      }
    }, 500) // Delay in milliseconds
  }

  // Dynamically call handleSearch() when the text in the search bar changes
  searchField.text.onChange { (_, _, newValue) =>
    debounceSearch()
  }

  val searchButton = new Button("Search") {
    onAction = _ => handleSearch()
    val spotifyGreen = Color.web("#1DB954")
    style =
      s"""
        -fx-background-color: #1DB954;
        -fx-text-fill: #FFFFFF;
        -fx-background-radius: 15;
        -fx-font-size: 14px;
        -fx-padding: 8 16;
      """
    effect = new DropShadow(5, spotifyGreen)
    onMouseEntered = _ =>
      style =
        s"""
          -fx-background-color: #1ED760;
          -fx-text-fill: #FFFFFF;
          -fx-background-radius: 15;
          -fx-font-size: 14px;
          -fx-padding: 8 16;
        """
    onMouseExited = _ =>
      style =
        s"""
          -fx-background-color: #1DB954;
          -fx-text-fill: #FFFFFF;
          -fx-background-radius: 15;
          -fx-font-size: 14px;
          -fx-padding: 8 16;
        """
  }

  val gridPane = new GridPane {
    hgap = 20
    vgap = 20
    padding = Insets(20)
    alignment = Pos.Center
  }

  val scrollPane = new ScrollPane {
    content = gridPane
    fitToWidth = true
    style = "-fx-background: #1E1E1E; -fx-border-color: transparent;"
    hbarPolicy = ScrollPane.ScrollBarPolicy.Never
    vbarPolicy = ScrollPane.ScrollBarPolicy.AsNeeded
  }

  val rootVBox = new VBox {
    spacing = 10
    padding = Insets(20)
    alignment = Pos.TopCenter
    style = "-fx-background-color: #1E1E1E;"

    children = Seq(
      new HBox {
        spacing = 10
        alignment = Pos.Center
        children = Seq(searchField, searchButton)
      },
      scrollPane,
      new Button("Playlists") {
        onAction = _ => {
          // Call the method to open the PlaylistManagerUI
          PlaylistManagerUI.showPlaylistManager() // Assuming PlaylistManagerUI has this method
        }
        style =
          s"""
          -fx-background-color: #1DB954;
          -fx-text-fill: #FFFFFF;
          -fx-background-radius: 15;
          -fx-font-size: 14px;
          -fx-padding: 8 16;
        """
        effect = new DropShadow(5, Color.web("#1DB954"))
        onMouseEntered = _ =>
          style =
            s"""
            -fx-background-color: #1ED760;
            -fx-text-fill: #FFFFFF;
            -fx-background-radius: 15;
            -fx-font-size: 14px;
            -fx-padding: 8 16;
          """
        onMouseExited = _ =>
          style =
            s"""
            -fx-background-color: #1DB954;
            -fx-text-fill: #FFFFFF;
            -fx-background-radius: 15;
            -fx-font-size: 14px;
            -fx-padding: 8 16;
          """
      }
    )
  }





  def handleSearch(): Unit = {
    val query = searchField.text.value.trim

    if (query.isEmpty) {
      FirebaseUtils.fetchAllSongs().onComplete {
        case Success(songs) =>
          val songDataList = songs.map { song =>
            SongData(
              title = song.getOrElse("title", "Unknown").toString,
              imagePath = song.getOrElse("imagePath", "Unknown").toString,
              filePath = song.getOrElse("filePath", "Unknown").toString
            )
          }
          Platform.runLater {
            updateUI(songDataList)
          }
        case Failure(exception) =>
          println(s"Failed to fetch all songs: ${exception.getMessage}")
      }
    } else {
      println(s"Searching for song: $query")

      val replyActor = ActorSystem(Behaviors.receiveMessage[List[Map[String, Any]]] { songs =>
        val songDataList = songs.map { song =>
          SongData(
            title = song.getOrElse("title", "Unknown").toString,
            imagePath = song.getOrElse("imagePath", "Unknown").toString,
            filePath = song.getOrElse("filePath", "Unknown").toString
          )
        }
        Platform.runLater {
          updateUI(songDataList)
        }
        Behaviors.stopped
      }, "SearchReplyActor")

      systemIntegrator ! SystemIntegratorActor.RouteToSongService(SearchSong(query, replyActor))
    }
  }

  // Fetch songs from Firebase and update the UI
  def fetchSongs(): Unit = {
    FirebaseUtils.fetchAllSongs().onComplete {
      case Success(songs) =>
        val songDataList = songs.map { song =>
          val title = song.getOrElse("title", "Unknown").toString
          val imagePath = song.getOrElse("imagePath", "Unknown").toString
          val filePath = song.getOrElse("filePath", "Unknown").toString
          SongData(title, imagePath, filePath)
        }
        Platform.runLater {
          updateUI(songDataList)
        }
      case Failure(exception) =>
        println(s"Failed to fetch songs: ${exception.getMessage}")
    }
  }

  def updateUI(songs: List[SongData]): Unit = {
    Platform.runLater {
      if (songs.isEmpty) {
        gridPane.children.clear()
        gridPane.add(new Label("No songs available."), 0, 0)
      } else {
        gridPane.children.clear()
        songs.zipWithIndex.foreach { case (song, index) =>
          val row = index / 3
          val col = index % 3
          gridPane.add(createSongBox(song), col, row)
        }
      }
    }
  }

  def createSongBox(song: SongData): VBox = {
    val imageView = new ImageView(new Image(new FileInputStream(song.imagePath))) {
      fitWidth = 100
      fitHeight = 100
      preserveRatio = true
      style = "-fx-background-radius: 15; -fx-border-radius: 15;"
    }

    imageView.onMouseClicked = _ => {
      println(s"Clicked on song: ${song.title}")
      MusicPlayerUI.show(song)
    }

    val titleLabel = new Label(song.title) {
      style =
        s"""
          -fx-text-fill: #FFFFFF;
          -fx-font-size: 14px;
        """
      alignment = Pos.Center
    }

    new VBox {
      spacing = 10
      alignment = Pos.Center
      padding = Insets(10)
      style = "-fx-background-color: #333333; -fx-background-radius: 15;"
      children = Seq(imageView, titleLabel)
    }
  }

  def startSongLibraryUI(): Unit = {
    fetchSongs()
    Platform.runLater {
      val stage = new javafx.stage.Stage
      stage.setTitle("Song Library")
      stage.setScene(createSongLibraryScene())
      stage.show()
    }
  }

  def createSongLibraryScene(): Scene = {
    new Scene(600, 400) {
      root = rootVBox
    }

  }

  def showSpinner(): Scene = {
    val loadingSpinner = new ProgressIndicator {
      style = "-fx-progress-color: #1DB954;" // Green color for the spinner
    }

    val spinnerPane = new StackPane {
      prefWidth = 600
      prefHeight = 400
      children = loadingSpinner
      alignment = Pos.Center
    }

    // Initially, display the spinner
    val sceneWithSpinner = new Scene(600, 400) {
      root = spinnerPane
    }

    sceneWithSpinner
  }


  // Launch the UI without using main
  def launchUI(): Unit = {
    Platform.runLater {
      startSongLibraryUI()
    }
  }

  // Call this method to start the UI
  launchUI()
}