package main

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import actors.{MusicPlayerActor, PlaylistServiceActor, SongLibraryActor, SystemIntegratorActor, UserServiceActor}
import actors.UserServiceActor.{LoginUser, RegisterUser, ValidateSession, DeleteUser, UpdatePassword}
import utils.FirebaseUtils
import akka.actor.typed.ActorRef
import protocols.SongProtocols.{AddSong, SearchSong}

object Main extends App {
  // Initialize Firebase
  FirebaseUtils.initializeFirebase()

  // Initialize UserServiceActor - Create the main ActorSystem once
  val userService: ActorRef[UserServiceActor.Command] = ActorSystem(UserServiceActor(), "UserServiceActor")
  val songLibrary: ActorRef[protocols.SongProtocols.Command] = ActorSystem(SongLibraryActor(), "SongLibraryActor")
  val musicPlayerActor: ActorSystem[protocols.SongProtocols.Command] = ActorSystem(MusicPlayerActor(), "MusicPlayerActor")
  val playlistServiceActor: ActorRef[protocols.PlaylistProtocols.Command] = ActorSystem(PlaylistServiceActor(), "PlaylistServiceActor")

  // Initialize SystemIntegratorActor, passing all required actors
  implicit val systemIntegrator: ActorRef[SystemIntegratorActor.Command] = ActorSystem(
    SystemIntegratorActor(userService, songLibrary, playlistServiceActor, musicPlayerActor),
    "SystemIntegratorActor"
  )

  // Interactive Menu
  println("Welcome to Spotify Distributed System")
  println("1. Register a new user")
  println("2. Login existing user")
  println("3. Search for a song")
  println("4. Validate session")

  val choice = scala.io.StdIn.readInt()

  choice match {
    case 1 =>
      println("Enter username (email):")
      val username = scala.io.StdIn.readLine()
      println("Enter password:")
      val password = scala.io.StdIn.readLine()

      // Create a temporary actor to receive the reply for registration
      val replyActor = ActorSystem(Behaviors.receiveMessage[String] { response =>
        println(response) // Print the response from Firebase
        Behaviors.stopped // Stop the actor after receiving the response
      }, "RegisterReplyActor")

      // Send RegisterUser command to the UserServiceActor
      userService ! RegisterUser(username, password, replyActor)

    case 2 =>
      println("Enter username (email):")
      val username = scala.io.StdIn.readLine()
      println("Enter password:")
      val password = scala.io.StdIn.readLine()

      // Create a temporary actor to receive the reply for login
      val replyActor = ActorSystem(Behaviors.receiveMessage[String] { response =>
        println(response) // Print the response from Firebase
        Behaviors.stopped // Stop the actor after receiving the response
      }, "LoginReplyActor")

      // Send LoginUser command to the UserServiceActor
      userService ! LoginUser(username, password, replyActor)

    case 3 =>
      println("Enter song title to search:")
      val title = scala.io.StdIn.readLine()

      def formatSongInfo(song: Map[String, Any]): String = {
        s"Title: ${song.getOrElse("title", "Unknown")}, " +
          s"Artist: ${song.getOrElse("artist", "Unknown")}, " +
          s"Genre: ${song.getOrElse("genre", "Unknown")}, " +
          s"Duration: ${song.getOrElse("duration", "Unknown")}, " +
          s"FilePath: ${song.getOrElse("filePath", "Unknown")}, " +
          s"ImagePath: ${song.getOrElse("imagePath", "Unknown")} " +
          s"SongId: ${song.getOrElse("id", "Unknown")}"
      }

      val replyActor = ActorSystem(Behaviors.receiveMessage[List[Map[String, Any]]] { songs =>
        if (songs.nonEmpty) {
          println(s"Number of songs found: ${songs.size}")
          songs.foreach(song => println(formatSongInfo(song)))
        } else {
          println("No songs found.")
        }
        Behaviors.stopped
      }, "SearchReplyActor")

      // Route SearchSong command to SongLibraryActor via SystemIntegratorActor
      systemIntegrator ! SystemIntegratorActor.RouteToSongService(
        SearchSong(title, replyActor)
      )

    case 4 =>
      println("Enter session token to validate:")
      val token = scala.io.StdIn.readLine()

      val replyActor = ActorSystem(Behaviors.receiveMessage[Boolean] { isValid =>
        println(if (isValid) "Session is valid." else "Session is invalid.")
        Behaviors.stopped
      }, "ValidateSessionReplyActor")

      // Send ValidateSession command to the UserServiceActor
      userService ! ValidateSession(token, replyActor)

    case 5 =>
      println("Enter username (email):")
      val username = scala.io.StdIn.readLine()
      println("Enter new password:")
      val newPassword = scala.io.StdIn.readLine()

      val replyActor = ActorSystem(Behaviors.receiveMessage[String] { response =>
        println(response) // Print the response from Firebase
        Behaviors.stopped
      }, "UpdatePasswordReplyActor")

      // Send UpdatePassword command to the UserServiceActor
      userService ! UpdatePassword(username, newPassword, replyActor)

    case 6 =>
      println("Enter username (email) to delete:")
      val username = scala.io.StdIn.readLine()

      val replyActor = ActorSystem(Behaviors.receiveMessage[String] { response =>
        println(response) // Print the response from Firebase
        Behaviors.stopped
      }, "DeleteUserReplyActor")

      // Send DeleteUser command to the UserServiceActor
      userService ! DeleteUser(username, replyActor)

    case _ =>
      println("Invalid choice")
  }
}
