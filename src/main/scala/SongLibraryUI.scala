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
    SystemIntegratorActor(null, null, musicPlayerActor), // Adjusted to 3 parameters as required
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
    val imageView = try {
      new ImageView(new Image(new FileInputStream(song.imagePath))) {
        fitWidth = 100
        fitHeight = 100
        preserveRatio = true
      }
    } catch {
      case e: Exception =>
        println(s"Error loading image for song: ${song.title}. Using placeholder image.")
        new ImageView(new Image(getClass.getResourceAsStream("/placeholder.jpg"))) {
          fitWidth = 100
          fitHeight = 100
          preserveRatio = true
        }
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
