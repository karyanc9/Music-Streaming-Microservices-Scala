package main

import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.VBox
import scalafx.stage.Stage
import akka.actor.typed.ActorRef
import actors.SystemIntegratorActor
import protocols.SongProtocols.{PauseSong, PlaySong}
import scalafx.geometry.Pos

import java.io.FileInputStream

object MusicPlayerUI {
  def show(song: SongData)(implicit systemIntegrator: ActorRef[SystemIntegratorActor.Command]): Unit = {
    val stage = new Stage {
      title = s"Music Player - ${song.title}"
      scene = new Scene(400, 400) {
        val imageView = new ImageView(new Image(new FileInputStream(song.imagePath))) {
          fitWidth = 200
          fitHeight = 200
          preserveRatio = true
        }

        val playButton = new Button("Play") {
          onAction = _ => {
            println(s"Playing song: ${song.title}")
            systemIntegrator ! SystemIntegratorActor.RouteToMusicPlayer(
              PlaySong(song.title, song.filePath, null)
            )
          }
        }

        val pauseButton = new Button("Pause") {
          onAction = _ => {
            println(s"Pausing song: ${song.title}")
            systemIntegrator ! SystemIntegratorActor.RouteToMusicPlayer(
              PauseSong(song.title, null)
            )
          }
        }

        root = new VBox {
          spacing = 20
          alignment = Pos.Center
          children = Seq(imageView, new Label(song.title), playButton, pauseButton)
        }
      }
    }

    stage.show()
  }
}


//
//import scalafx.scene.Scene
//import scalafx.scene.control.{Button, Label}
//import scalafx.scene.image.{Image, ImageView}
//import scalafx.scene.layout.VBox
//import scalafx.stage.Stage
//import actors.SystemIntegratorActor
//import protocols.SongProtocols.{PauseSong, PlaySong}
//import akka.actor.typed.ActorRef
////import main.SongLibraryUI.SongData
//import scalafx.geometry.Pos
//
//import java.io.FileInputStream
//
//object MusicPlayerUI {
//  def show(song: SongData)(implicit systemIntegrator: ActorRef[SystemIntegratorActor.Command]): Unit = {
//    val stage = new Stage {
//      title = s"Music Player - ${song.title}"
//      scene = new Scene(400, 400) {
//        val imageView = new ImageView(new Image(new FileInputStream(song.imagePath))) {
//          fitWidth = 200
//          fitHeight = 200
//          preserveRatio = true
//        }
//
//        val playButton = new Button("Play") {
//          onAction = _ => {
//            println(s"Playing song: ${song.title}")
//            systemIntegrator ! SystemIntegratorActor.RouteToMusicPlayer(
//              PlaySong(song.title, song.filePath, null)
//            )
//          }
//        }
//
//        val pauseButton = new Button("Pause") {
//          onAction = _ => {
//            println(s"Pausing song: ${song.title}")
//            systemIntegrator ! SystemIntegratorActor.RouteToMusicPlayer(
//              PauseSong(song.title, null)
//            )
//          }
//        }
//
//        root = new VBox {
//          spacing = 20
//          alignment = Pos.Center
//          children = Seq(imageView, new Label(song.title), playButton, pauseButton)
//        }
//      }
//    }
//    stage.show()
//  }
//}
//