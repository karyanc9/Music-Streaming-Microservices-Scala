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
import scalafx.scene.layout.{BorderPane, GridPane, HBox, VBox}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.control.{Button, Label, TextField}
import scalafx.geometry.{Insets, Pos}
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
    SystemIntegratorActor(null, songLibrary, musicPlayerActor),
    "SystemIntegratorActor"
  )

  //private var allSongs: List[SongData] = List() //storing all the songs fetched form firebase

  //ui compononet
  val searchField = new TextField {
    promptText = "Search for a song..."
    prefWidth = 300
  }

  // Dynamically call handleSearch() when the text in the search bar changes
  searchField.text.onChange { (_, _, newValue) =>
    handleSearch() // Call handleSearch() whenever the search text changes
  }

  val searchButton = new Button("Search") {
    onAction = _ => handleSearch()
  }

  val gridPane = new GridPane {
    hgap = 20
    vgap = 20
    padding = Insets(20)
  }

  val rootVBox = new VBox {
    spacing = 10
    padding = Insets(20)
    alignment = Pos.TopCenter
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
    }

    imageView.onMouseClicked = _ => {
      println(s"Clicked on song: ${song.title}")
      MusicPlayerUI.show(song)
    }

    new VBox {
      spacing = 10
      alignment = Pos.Center
      padding = Insets(10)
      children = Seq(imageView, new Label(song.title))
    }
  }

  // Update the UI with the fetched songss
  //  def updateUI(songs: List[SongData])(implicit systemIntegrator: ActorSystem[SystemIntegratorActor.Command]): Unit = {
  //    Platform.runLater {
  //      val gridPane = new GridPane {
  //        hgap = 20
  //        vgap = 20
  //        padding = Insets(20)
  //      }
  //
  //      songs.zipWithIndex.foreach { case (song, index) =>
  //        val row = index / 3
  //        val col = index % 3
  //        gridPane.add(createSongBox(song), col, row)
  //      }
  //
  //      stage.scene = new Scene {
  //        root = gridPane
  //      }
  //    }
  //  }

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


  // Define the primary stage
  stage = new PrimaryStage {
    title = "Song Library"
    scene = new Scene(600, 400) {
      content = new Label("Loading...")
    }
  }

  // Start fetching songs when the application launches
  fetchSongs
}




//
//
//import scalafx.application.{JFXApp, JFXApp3, Platform}
//import scalafx.application.JFXApp.PrimaryStage
//import scalafx.scene.Scene
//import scalafx.scene.layout.{GridPane, VBox}
//import scalafx.scene.image.{Image, ImageView}
//import scalafx.scene.control.Label
//import scalafx.geometry.{Insets, Pos}
//import utils.FirebaseUtils
//import java.io.FileInputStream
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.util.{Failure, Success}
//import actors.SystemIntegratorActor
//import protocols.SongProtocols.{PauseSong, PlaySong}
//import akka.actor.typed.ActorRef
//
//object SongLibraryUI extends JFXApp {
//
//  case class SongData(title: String, imagePath: String, filePath: String)
//
//  def fetchSongs(): Unit = {
//    println("Starting to fetch songs from Firebase...")
//    FirebaseUtils.fetchAllSongs().onComplete {
//      case Success(songs) =>
//        println(s"Fetched ${songs.size} songs from Firebase.")
////        val songDataList = songs.map(song =>
////          SongData(
////            title = song.getOrElse("title", "Unknown").toString,
////            imagePath = song.getOrElse("imagePath", "Unknown").toString
////          )
////        )
//        val songDataList = songs.map { song =>
//          val title = song.getOrElse("title", "Unknown").toString
//          val imagePath = song.getOrElse("imagePath", "Unknown").toString
//          val filePath = song.getOrElse("filePath", "Unknown").toString
//          println(s"Mapping song - Title: $title, ImagePath: $imagePath, FilePath: $filePath")
//          SongData(title = title, imagePath = imagePath, filePath = filePath)
//        }
//        println(s"Mapped songs to SongData: $songDataList")
//        Platform.runLater {
//          println("Calling updateUI with mapped songs...")
//          updateUI(songDataList)
//        }
//        //updateUI(songDataList)
//
//      case Failure(exception) =>
//        println(s"Failed to fetch songs: ${exception.getMessage}")
//    }
//  }
//
//  def createSongBox(song: SongData): VBox = {
//    println(s"Creating song box for: ${song.title}, ImagePath: ${song.imagePath}")
//    try {
//      val image = new Image(new FileInputStream(song.imagePath))
//      val imageView = new ImageView(image) {
//        fitWidth = 100
//        fitHeight = 100
//        preserveRatio = true
//      }
//
//      val title = new Label(song.title)
//
//      println(s"Successfully created song box for: ${song.title}")
//      new VBox {
//        spacing = 10
//        alignment = Pos.Center
//        padding = Insets(10)
//        children = Seq(imageView, title)
//      }
//    } catch {
//      case e: Exception =>
//        println(s"Error loading image for song: ${song.title}, ${e.getMessage}")
//        new VBox(new Label(song.title))
//    }
////    val image = new Image(getClass.getResourceAsStream(s"/${song.imagePath}"))
////    val imageView = new ImageView(image) {
////      fitWidth = 100
////      fitHeight = 100
////      preserveRatio = true
////    }
////
////    val title = new Label(song.title)
////
////    new VBox {
////      spacing = 10
////      alignment = Pos.Center
////      padding = Insets(10)
////      children = Seq(imageView, title)
////    }
//  }
//
//  def updateUI(songs: List[SongData]): Unit = {
//    println(s"Updating UI with ${songs.size} songs.")
//    songs.foreach(song => println(s"Adding song: ${song.title}, ImagePath: ${song.imagePath}"))
//
//    if (songs.isEmpty) {
//      println("No songs available to display.")
//      return
//    }
//
//    val gridPane = new GridPane {
//      hgap = 20
//      vgap = 20
//      padding = Insets(20)
//    }
//
//    songs.zipWithIndex.foreach { case (song, index) =>
//      val row = index / 3
//      val col = index % 3
//      println(s"Adding song to GridPane: ${song.title} at row $row, col $col")
//      gridPane.add(createSongBox(song), col, row)
//    }
//
//    println("Replacing the scene with the updated GridPane...")
//    stage.scene = new Scene {
//      root = gridPane
//    }
//  }
//
//  stage = new PrimaryStage {
//    title = "Song Library"
//    scene = new Scene(600, 400) {
//      content = new Label("Loading...")
//    }
//  }
//
////   val mockSongs = List(
////     SongData("Mamma Mia", "songs/song1.jpg"),
////     SongData("Billie Jean", "songs/song2.jpg")
////   )
////   updateUI(mockSongs)
//
//
//  fetchSongs()
//}
//
//import scala.concurrent.Await
//import scala.concurrent.duration._
//
////object FirebaseTest extends App {
////  FirebaseUtils.initializeFirebase()
////
////  val fetchSongsFuture = FirebaseUtils.fetchAllSongs()
////
////  try {
////    val songs = Await.result(fetchSongsFuture, 30.seconds) // Wait for up to 10 seconds
////    println(s"Fetched songs: $songs")
////    songs.foreach(song => println(s"Song data: $song"))
////  } catch {
////    case e: Exception =>
////      println(s"Failed to fetch songs: ${e.getMessage}")
////  }
////}