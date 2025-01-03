package main

import akka.actor.typed.ActorSystem
import actors.{MusicPlayerActor, SongLibraryActor, SystemIntegratorActor}
import akka.actor.typed.scaladsl.Behaviors
import protocols.SongProtocols
import protocols.SongProtocols.SearchSong
import scalafx.Includes.jfxSceneProperty2sfx
import scalafx.application.{JFXApp, Platform}
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, GridPane, HBox, StackPane, VBox}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.control.{Button, Label, ProgressIndicator, TextField}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.effect.DropShadow
import scalafx.scene.paint.Color
import scalafx.scene.text.Font
import utils.FirebaseUtils

import java.io.FileInputStream
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

case class SongData(title: String, imagePath: String, filePath: String)

object SongLibraryUI extends JFXApp {

  // Initialize the actors within the SongLibraryUI - libraeyy
  val songLibrary: ActorSystem[protocols.SongProtocols.Command] = ActorSystem(SongLibraryActor(), "SongLibraryActor")
  val musicPlayerActor: ActorSystem[SongProtocols.Command] = ActorSystem(MusicPlayerActor(), "MusicPlayerActor")
  implicit val systemIntegrator: ActorSystem[SystemIntegratorActor.Command] = ActorSystem(
    SystemIntegratorActor(null, songLibrary, null, musicPlayerActor),
    "SystemIntegratorActor"
  )

  //private var allSongs: List[SongData] = List() //storing all the songs fetched form firebase

  //ui compononet
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

  // Dynamically call handleSearch() when the text in the search bar changes
  searchField.text.onChange { (_, _, newValue) =>
    handleSearch() // Call handleSearch() whenever the search text changes
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
          -fx-background-radius: 15; /* Ensure the radius is maintained on hover */
          -fx-font-size: 14px;
          -fx-padding: 8 16;
        """
    onMouseExited = _ =>
      style =
        s"""
          -fx-background-color: #1DB954;
          -fx-text-fill: #FFFFFF;
          -fx-background-radius: 15; /* Ensure the radius is maintained when mouse leaves */
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
      gridPane
    )
  }


  //search for songs
  def handleSearch1(): Unit = {
    val query = searchField.text.value
    if (query.isEmpty) {
      println("Search query is empty. Fetching all songs...")
      fetchSongs // Fetch all songs if no search query is provided
    } else {
      println(s"Searching for song: $query")

      // Reply Actor to handle the search result
      val replyActor = ActorSystem(Behaviors.receiveMessage[List[Map[String, Any]]] { songs =>
        println(s"Received ${songs.size} songs from search query.")
        val songDataList = songs.map { song =>
          val title = song.getOrElse("title", "Unknown").toString
          val imagePath = song.getOrElse("imagePath", "Unknown").toString
          val filePath = song.getOrElse("filePath", "Unknown").toString
          println(s"Song - Title: $title, ImagePath: $imagePath, FilePath: $filePath")
          SongData(title, imagePath, filePath)
        }
        Platform.runLater {
          println("Updating UI with search results.")
          updateUI(songDataList)
        }
        Behaviors.stopped
      }, "SearchReplyActor")

      systemIntegrator ! SystemIntegratorActor.RouteToSongService(
        SearchSong(query, replyActor))
    }
  }

  def handleSearch3(): Unit = {
    val query = searchField.text.value.trim
    if (query.nonEmpty) {
      println(s"Searching for song: $query") // Debugging log

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

      // Send the search request to SystemIntegratorActor
      systemIntegrator ! SystemIntegratorActor.RouteToSongService(SearchSong(query, replyActor))
    }
  }

  def handleSearch(): Unit = {
    val query = searchField.text.value.trim

    if (query.isEmpty) {
      // If the search field is empty, fetch all songs
      println("Search bar cleared. Fetching all songs...")

      // Use FirebaseUtils to fetch all songs
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
            updateUI(songDataList) // Update the UI with all songs
          }
        case Failure(exception) =>
          println(s"Failed to fetch all songs: ${exception.getMessage}")
      }
    } else {
      // Otherwise, perform the search
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
          updateUI(songDataList) // Update the UI with searched results
        }
        Behaviors.stopped
      }, "SearchReplyActor")

      // Send the search request to SystemIntegratorActor
      systemIntegrator ! SystemIntegratorActor.RouteToSongService(SearchSong(query, replyActor))
    }
  }


  // Fetch songs from Firebase and update the UI
  def fetchSongs(implicit systemIntegrator: ActorSystem[SystemIntegratorActor.Command]): Unit = {
    println("Starting to fetch songs from Firebase...")
    FirebaseUtils.fetchAllSongs().onComplete {
      case Success(songs) =>
        val songDataList = songs.map { song =>
          val title = song.getOrElse("title", "Unknown").toString
          val imagePath = song.getOrElse("imagePath", "Unknown").toString
          val filePath = song.getOrElse("filePath", "Unknown").toString
          println(s"Mapping song - Title: $title, ImagePath: $imagePath, FilePath: $filePath")
          SongData(title, imagePath, filePath)
        }
        Platform.runLater {
          updateUI(songDataList)
        }
      case Failure(exception) =>
        println(s"Failed to fetch songs: ${exception.getMessage}")
    }
  }

  // Create a song box with image and title
  def createSongBox(song: SongData)(implicit systemIntegrator: ActorSystem[SystemIntegratorActor.Command]): VBox = {
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

  // Update the UI with the fetched songs
  def updateUI(songs: List[SongData])(implicit systemIntegrator: ActorSystem[SystemIntegratorActor.Command]): Unit = {
    Platform.runLater {
      if (songs.isEmpty) {
        println("No songs available to display.")
        gridPane.children.clear() // Clear previous content
        gridPane.add(new Label("No songs available."), 0, 0)
      } else {
        println(s"Displaying ${songs.size} songs.")
        gridPane.children.clear() // Clear previous content
        songs.zipWithIndex.foreach { case (song, index) =>
          val row = index / 3
          val col = index % 3
          gridPane.add(createSongBox(song), col, row)
        }
      }

      // Ensure the rootVBox and gridPane are displayed properly
      rootVBox.children = Seq(
        new HBox {
          spacing = 10
          alignment = Pos.Center
          children = Seq(searchField, searchButton)
        },
        gridPane
      )

      // Check if the scene's root is not set, and set it only if necessary
      if (stage.scene == null || stage.scene.root != rootVBox) {
        stage.scene = new Scene(600, 400) {
          root = rootVBox
        }
      }

    }
  }

  val loadingSpinner = new ProgressIndicator {
    style = "-fx-progress-color: #1DB954;"
  }

  val spinnerPane = new StackPane {
    prefWidth = 600
    prefHeight = 400
    children = loadingSpinner
    alignment = Pos.Center
  }

  // Define the primary stage
  stage = new PrimaryStage {
    title = "Song Library"
    scene = new Scene(600, 400) {
      content = new VBox {
        alignment = Pos.Center
        children = Seq(spinnerPane)
      }
      //content = new Label("Loading...")
    }
  }

  // Start fetching songs when the application launches
  fetchSongs
}