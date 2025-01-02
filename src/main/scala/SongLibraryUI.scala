package main

import akka.actor.typed.ActorSystem
import actors.{MusicPlayerActor, SystemIntegratorActor}
import protocols.SongProtocols
import scalafx.application.{JFXApp, Platform}
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.layout.{GridPane, VBox}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.control.Label
import scalafx.geometry.{Insets, Pos}
import utils.FirebaseUtils

import java.io.FileInputStream
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

case class SongData(title: String, imagePath: String, filePath: String)

object SongLibraryUI extends JFXApp {

  // Initialize the actors within the SongLibraryUI
  val musicPlayerActor: ActorSystem[SongProtocols.Command] = ActorSystem(MusicPlayerActor(), "MusicPlayerActor")
  implicit val systemIntegrator: ActorSystem[SystemIntegratorActor.Command] = ActorSystem(
    SystemIntegratorActor(null, null, null, musicPlayerActor),
    "SystemIntegratorActor"
  )

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

  // Update the UI with the fetched songs
  def updateUI(songs: List[SongData])(implicit systemIntegrator: ActorSystem[SystemIntegratorActor.Command]): Unit = {
    Platform.runLater {
      val gridPane = new GridPane {
        hgap = 20
        vgap = 20
        padding = Insets(20)
      }

      songs.zipWithIndex.foreach { case (song, index) =>
        val row = index / 3
        val col = index % 3
        gridPane.add(createSongBox(song), col, row)
      }

      stage.scene = new Scene {
        root = gridPane
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