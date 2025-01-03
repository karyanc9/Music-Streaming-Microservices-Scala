package main

import akka.actor.typed.ActorSystem
import actors.{MusicPlayerActor, PlaylistServiceActor, SongLibraryActor, SystemIntegratorActor}
import protocols.{PlaylistProtocols, SongProtocols}
import protocols.PlaylistProtocols.CreatePlaylist
import scalafx.application.{JFXApp, Platform}
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.layout.{HBox, VBox}
import scalafx.scene.control.{Alert, Button, ButtonType, Dialog, Label, ListView, ProgressIndicator, ScrollPane, TextField}
import scalafx.geometry.{Insets, Pos}
import utils.FirebaseUtils
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent.duration._
import akka.actor.typed.ActorRef
import main.SongLibraryUI.rootVBox

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scalafx.collections.ObservableBuffer
import scalafx.scene.paint.Color

import scala.jdk.CollectionConverters._
import scala.reflect.internal.util.NoSourceFile.content

object PlaylistManagerUI extends JFXApp {

  FirebaseUtils.initializeFirebase()

  lazy val songLibrary: ActorSystem[protocols.SongProtocols.Command] = ActorSystem(SongLibraryActor(), "SongLibraryActor")
  lazy val musicPlayerActor: ActorSystem[SongProtocols.Command] = ActorSystem(MusicPlayerActor(), "MusicPlayerActor")
  lazy val playlistServiceActor: ActorSystem[PlaylistProtocols.Command] = ActorSystem(PlaylistServiceActor(), "PlaylistServiceActor")
  lazy implicit val systemIntegrator: ActorSystem[SystemIntegratorActor.Command] = ActorSystem(
    SystemIntegratorActor(null, songLibrary, playlistServiceActor, musicPlayerActor),
    "SystemIntegratorActor"
  )


  case class PlaylistData(id: String, name: String, songCount: Int)

  def fetchPlaylists(implicit systemIntegrator: ActorSystem[SystemIntegratorActor.Command]): Unit = {
    FirebaseUtils.getAllPlaylists.onComplete {
      case Success(playlists) =>
        try {
          val playlistDataList = playlists.map { playlist =>
            val id = playlist.getOrElse("id", "").toString
            val name = playlist.getOrElse("name", "Unknown").toString

            // Safely convert the "songs" field to a Scala Map and count the number of songs
            val songCount = playlist.get("songs") match {
              case Some(songMap: java.util.Map[_, _]) =>
                // Count the number of keys in the songs map (each key represents a song)
                songMap.size()
              case _ => 0 // If "songs" is missing or not a map, default to 0
            }

            PlaylistData(id, name, songCount)
          }

          Platform.runLater {
            updateUI(playlistDataList)
          }
        } catch {
          case ex: Exception =>
            println(s"Error processing playlist data: ${ex.getMessage}")
            Platform.runLater {
              updateUI(List()) // Fallback to an empty list
            }
        }
      case Failure(exception) =>
        println(s"Failed to fetch playlists: ${exception.getMessage}")
        Platform.runLater {
          updateUI(List()) // Show empty list if fetching fails
        }
    }
  }

  def updateUI(playlists: List[PlaylistData]): Unit = {
    Platform.runLater {
      // Set fixed dimensions for the content area
      val fixedWidth = 600.0
      val fixedHeight = 600.0

      // Map playlists into UI elements
      val playlistBoxes = playlists.map { playlist =>
        new VBox {
          spacing = 20
          alignment = Pos.TopCenter
          padding = Insets(15)
          prefWidth = fixedWidth - 40 // Adjust to fit within the container
          style = """
                    | -fx-background-color: #2A2A2A;
                    | -fx-border-color: #444;
                    | -fx-border-radius: 15;
                    | -fx-background-radius: 15;
                    | -fx-padding: 15;
                    | -fx-effect: dropshadow(three-pass-box, #000000, 10, 0, 0, 2);
                """.stripMargin

          children = Seq(
            new Label(s"${playlist.name}") {
              style = """
                        | -fx-text-fill: white;
                        | -fx-font-size: 20px;
                        | -fx-font-weight: bold;
                        | -fx-padding: 10 0;
                """.stripMargin
            },

            new Label(s"${playlist.songCount} songs") {
              style = """
                        | -fx-text-fill: #B0B0B0;
                        | -fx-font-size: 14px;
                        | -fx-padding: 5 0;
                """.stripMargin
            },

            // Create a VBox for buttons stacked vertically
            new VBox {
              spacing = 15
              alignment = Pos.Center
              children = Seq(
                new Button("Go to Songs") {
                  style = """
                            | -fx-background-color: #1DB954;
                            | -fx-text-fill: white;
                            | -fx-font-size: 14px;
                            | -fx-border-radius: 25;
                            | -fx-background-radius: 25;
                            | -fx-padding: 8 25;
                            | -fx-effect: dropshadow(three-pass-box, #000000, 5, 0, 0, 2);
                        """.stripMargin
                  onAction = _ => goToSongsPage(playlist.id)
                },
                new Button("Delete Playlist") {
                  style = """
                            | -fx-background-color: #FF5555;
                            | -fx-text-fill: white;
                            | -fx-font-size: 14px;
                            | -fx-border-radius: 25;
                            | -fx-background-radius: 25;
                            | -fx-padding: 8 25;
                            | -fx-effect: dropshadow(three-pass-box, #000000, 5, 0, 0, 2);
                        """.stripMargin
                  onAction = _ => deletePlaylist(playlist.id)
                }
              )
            }
          )
        }
      }

      // Create the "Create Playlist" button below the header area, with enhanced style
      val createPlaylistButton = new Button("Create Playlist") {
        style = """
                  |          -fx-background-color: #1DB954;
                  |          -fx-text-fill: #FFFFFF;
                  |          -fx-font-size: 14px;
                  |          -fx-background-radius: 15;
      """.stripMargin
        onAction = _ => showCreatePlaylistDialog()
      }

      val goBackButton = new Button("Back") {
        style =
          """
            |          -fx-background-color: #1DB954;
            |          -fx-text-fill: #FFFFFF;
            |          -fx-font-size: 14px;
            |          -fx-background-radius: 15;
                """.stripMargin
        onAction = _ => goBackToPreviousPage() // Call the goBackToPreviousPage method when clicked
      }

      // Main container for the playlists (inside a ScrollPane)
      val playlistsContainer = new VBox {
        spacing = 20
        alignment = Pos.TopCenter
        prefWidth = fixedWidth
        padding = Insets(20)
        style = "-fx-background-color: #2A2A2A;"  // Set the background color of the container to dark grey
        children = playlistBoxes
      }

      // Wrap the playlists container inside a ScrollPane
      val scrollableContainer = new ScrollPane {
        content = playlistsContainer
        fitToWidth = true // Ensure the content stretches to the width of the container
        style = "-fx-background-color: transparent;" // Keep the ScrollPane's background transparent
      }

      // Main layout with the "Create Playlist" button placed below a margin
      val mainContainer = new VBox {
        spacing = 20
        alignment = Pos.TopCenter
        prefWidth = fixedWidth
        prefHeight = fixedHeight
        style = "-fx-background-color: #2A2A2A;"  // Set the main container background to dark grey
        children = Seq(
          // Horizontal box for the "Go Back" and "Create Playlist" buttons
          new HBox {
            spacing = 20 // Space between the buttons
            alignment = Pos.Center // Center the buttons horizontally
            children = Seq(goBackButton, createPlaylistButton) // Add the buttons here
          },
          // Scrollable container for the playlists
          scrollableContainer // This remains vertically aligned below the buttons
        )
      }

      // Update the scene with the fixed layout
      stage.scene = new Scene(fixedWidth, fixedHeight) {
        root = mainContainer
        fill = Color.DarkGrey // Set the overall scene background to dark grey
      }
    }
  }

  def goToSongsPage(playlistId: String): Unit = {
    println(s"Navigating to songs page for playlist with ID: $playlistId")

    val callback = () => {
      // Assuming `fetchPlaylistSongs` is the method that updates the PlaylistManager page
      PlaylistManagerUI.fetchPlaylists(systemIntegrator) // Replace with actual function to refresh your playlist manager
    }
    PlaylistSongsUI.show(playlistId, playlistServiceActor, callback)
  }

  def goBackToPreviousPage(): Unit = {
    println("Navigating back to the previous page.")

    // Get the song library scene from the SongLibraryUI object
    val songLibraryScene = SongLibraryUI.createSongLibraryScene()

    // Set the new scene to the stage
    stage.scene = songLibraryScene
  }


  def showPlaylistManager(): Unit = {
    if (stage == null) {
      // Initialize the stage if it's not already done
      stage = new PrimaryStage {
        title = "Playlist Library"
        scene = new Scene(600, 600) {
          fill = Color.DarkGrey
          content = new ProgressIndicator() {
            minHeight = 100
            minWidth = 100
            maxWidth = 100
            maxHeight = 100
            progress = -1 // Infinite progress
          }
        }
      }
    }
    stage.show() // Show the primary stage of PlaylistManagerUI
    fetchPlaylists
  }

  def deletePlaylist(playlistId: String): Unit = {
    val alert = new Alert(Alert.AlertType.Confirmation) {
      title = "Confirm Delete"
      headerText = s"Are you sure you want to delete this playlist?"
    }

    alert.showAndWait() match {
      case Some(ButtonType.OK) =>
        println(s"Deleting playlist with ID: $playlistId")

        // Send the delete request to the playlist actor via SystemIntegrator
        systemIntegrator ! SystemIntegratorActor.RouteToPlaylistService(PlaylistProtocols.RemovePlaylist(playlistId))

        // Refresh the playlists (slight delay for Firebase consistency)
        Platform.runLater {
          Thread.sleep(500) // Optional delay
          fetchPlaylists
        }

      case _ =>
        println("Deletion cancelled.")
    }
  }

  def showCreatePlaylistDialog(): Unit = {
    val dialog = new Dialog[String]() {
      title = "Create Playlist"
      headerText = "Enter Playlist Name"
    }

    val textField = new TextField {
      promptText = "Playlist Name"
    }

    val dialogPane = dialog.dialogPane()
    dialogPane.setContent(new VBox {
      spacing = 10
      children = Seq(textField)
    })

    val createButton = ButtonType.OK
    val cancelButton = ButtonType.Cancel

    dialog.getDialogPane().getButtonTypes.addAll(createButton, cancelButton)

    dialog.resultConverter = dialogButton => {
      dialogButton match {
        case `createButton` =>
          val name = textField.text()
          if (name.nonEmpty) {
            println(s"Creating playlist: $name")
            // Send the message to the PlaylistServiceActor
            systemIntegrator ! SystemIntegratorActor.RouteToPlaylistService(PlaylistProtocols.CreatePlaylist(name))
            fetchPlaylists
            dialog.close()
          } else {
            println("Playlist name cannot be empty.")
          }
          null
        case _ =>
          println("Dialog cancelled.")
          null
      }
    }

    dialog.showAndWait()
  }


}
