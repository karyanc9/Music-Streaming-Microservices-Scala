package main

import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{HBox, VBox}
import scalafx.stage.Stage
import akka.actor.typed.ActorRef
import actors.SystemIntegratorActor
import main.SongLibraryUI.SongData
import protocols.SongProtocols.{PauseSong, PlaySong}
import scalafx.geometry.Pos
import models.SongInfo
import scalafx.scene.effect.DropShadow
import scalafx.scene.paint.Color

import java.io.FileInputStream


object MusicPlayerUI {

  // function to show the music player
  def show(song: SongData)(implicit systemIntegrator: ActorRef[SystemIntegratorActor.Command]): Unit = {
    val stage = new Stage {
      title = s"Music Player - ${song.title}"
      scene = new Scene(400, 400) {
        val imageView = new ImageView(new Image(new FileInputStream(song.imagePath))) {
          fitWidth = 200
          fitHeight = 200
          preserveRatio = true
          style = "-fx-background-radius: 15;"
        }

        val songLabel = new Label(song.title) {
          style =
            """
              -fx-text-fill: #FFFFFF;
              -fx-font-size: 18px;
              -fx-font-weight: bold;
              -fx-padding: 10;
            """
        }

        // playing the music
        val playButton = new Button("Play") {
          onAction = _ => {
            println(s"Playing song: ${song.title}")
            systemIntegrator ! SystemIntegratorActor.RouteToMusicPlayer(
              PlaySong(song.title, song.filePath, null)
            )
          }
          style =
            """
              -fx-background-color: #1DB954;
              -fx-text-fill: #FFFFFF;
              -fx-background-radius: 20;
              -fx-font-size: 14px;
              -fx-padding: 10 20;
            """
          effect = new DropShadow(5, Color.web("#1DB954"))
          onMouseEntered = _ =>
            style =
              """
                -fx-background-color: #1ED760;
                -fx-text-fill: #FFFFFF;
                -fx-background-radius: 20;
                -fx-font-size: 14px;
                -fx-padding: 10 20;
              """
          onMouseExited = _ =>
            style =
              """
                -fx-background-color: #1DB954;
                -fx-text-fill: #FFFFFF;
                -fx-background-radius: 20;
                -fx-font-size: 14px;
                -fx-padding: 10 20;
              """
        }

        // pausing the music
        val pauseButton = new Button("Pause") {
          onAction = _ => {
            println(s"Pausing song: ${song.title}")
            systemIntegrator ! SystemIntegratorActor.RouteToMusicPlayer(
              PauseSong(song.title, null)
            )
          }
          style =
            """
              -fx-background-color: #808080;
              -fx-text-fill: #FFFFFF;
              -fx-background-radius: 20;
              -fx-font-size: 14px;
              -fx-padding: 10 20;
            """
          effect = new DropShadow(5, Color.web("#A9A9A9"))
          onMouseEntered = _ =>
            style =
              """
                -fx-background-color: #B0B0B0;
                -fx-text-fill: #FFFFFF;
                -fx-background-radius: 20;
                -fx-font-size: 14px;
                -fx-padding: 10 20;
              """
          onMouseExited = _ =>
            style =
              """
                -fx-background-color: #808080;
                -fx-text-fill: #FFFFFF;
                -fx-background-radius: 20;
                -fx-font-size: 14px;
                -fx-padding: 10 20;
              """
        }

        val buttonContainer = new HBox {
          spacing = 20
          alignment = Pos.Center
          children = Seq(playButton, pauseButton)
        }

        root = new VBox {
          spacing = 20
          alignment = Pos.Center
          style =
            """
              -fx-background-color: #1E1E1E;
              -fx-padding: 20;
            """
          children = Seq(imageView, songLabel, buttonContainer)
        }
      }
    }

    stage.show()
  }

  // Overloaded show method for SongInfo
  def showWithSongInfo(song: SongInfo)(implicit systemIntegrator: ActorRef[SystemIntegratorActor.Command]): Unit = {
    show(SongData(song.title, song.imagePath, song.filePath))
  }
}

