package main

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{VBox, HBox, BorderPane}
import scalafx.scene.paint.Color
import scalafx.scene.text.Font
import services.MusicPlayerService

object Main extends JFXApp3 {

  override def start(): Unit = {
    val playerService = new MusicPlayerService("songs/")

    // UI Components
    val songTitle = new Label {
      text = "Select a Song"
      font = new Font("Arial", 20)
      textFill = Color.White
    }

    // ImageView to display album cover
    val albumCover = new ImageView {
      fitWidth = 200
      fitHeight = 200
      preserveRatio = true
    }

    // Define a method to create styled buttons
    def createStyledButton(text: String, defaultColor: String, hoverColor: String, clickedColor: String)(action: => Unit): Button = {
      val button = new Button(text) {
        onAction = _ => action
        style = s"-fx-font-size: 14px; -fx-padding: 10px; -fx-background-color: $defaultColor; -fx-text-fill: white; -fx-background-radius: 20px;"
      }

      button.onMouseEntered = _ => button.style = s"-fx-font-size: 14px; -fx-padding: 10px; -fx-background-color: $hoverColor; -fx-text-fill: white; -fx-background-radius: 20px;"
      button.onMouseExited = _ => button.style = s"-fx-font-size: 14px; -fx-padding: 10px; -fx-background-color: $defaultColor; -fx-text-fill: white; -fx-background-radius: 20px;"
      button.onMousePressed = _ => button.style = s"-fx-font-size: 14px; -fx-padding: 10px; -fx-background-color: $clickedColor; -fx-text-fill: white; -fx-background-radius: 20px;"
      button.onMouseReleased = _ => button.style = s"-fx-font-size: 14px; -fx-padding: 10px; -fx-background-color: $hoverColor; -fx-text-fill: white; -fx-background-radius: 20px;"

      button
    }

    val playButton = createStyledButton("Play", "#1DB954", "#1ED760", "#1AA34A") {
      playerService.play(songTitle, albumCover)
    }

    val pauseButton = createStyledButton("Pause", "#535353", "#6E6E6E", "#3F3F3F") {
      playerService.pause()
    }

    val nextButton = createStyledButton("Next", "#535353", "#6E6E6E", "#3F3F3F") {
      playerService.next(songTitle, albumCover)
    }

    val prevButton = createStyledButton("Previous", "#535353", "#6E6E6E", "#3F3F3F") {
      playerService.previous(songTitle, albumCover)
    }

    // Layouts
    val controlsBox = new HBox(15, prevButton, playButton, pauseButton, nextButton) {
      style = "-fx-alignment: center;"
    }

    val bottomBar = new BorderPane {
      center = controlsBox
      style = "-fx-background-color: #282828; -fx-padding: 20px;"
    }

    val mainLayout = new BorderPane {
      top = new VBox(10, songTitle) {
        style = "-fx-alignment: center; -fx-padding: 20px;"
      }
      center = albumCover
      bottom = bottomBar
      style = "-fx-background-color: #121212;"
    }

    // Configure the primary stage
    stage = new JFXApp3.PrimaryStage {
      title = "Simple Spotify Music Player"
      scene = new Scene(800, 600) { // Enlarged to tablet size
        root = mainLayout
      }
    }
  }
}

