package main

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import actors.{SongLibraryActor, UserServiceActor, SystemIntegratorActor}
import actors.UserServiceActor.{LoginUser, RegisterUser}
import utils.FirebaseUtils
import akka.actor.typed.ActorRef
import protocols.SongProtocols.{AddSong, SearchSong}

object Main extends App {
  // Initialize Firebase
  FirebaseUtils.initializeFirebase()

  // Initialize UserServiceActor - Create the main ActorSystem once
  val userService: ActorRef[UserServiceActor.Command] = ActorSystem(UserServiceActor(), "UserServiceActor")
  val songLibrary: ActorRef[protocols.SongProtocols.Command] = ActorSystem(SongLibraryActor(), "SongLibraryActor")

  // Initialize SongLibraryActor via SystemIntegratorActor
  val systemIntegrator: ActorRef[SystemIntegratorActor.Command] = ActorSystem(
    SystemIntegratorActor(userService, songLibrary ,null),
    "SystemIntegratorActor"
  )

  // Interactive Menu
  println("Welcome to Spotify Distributed System")
  println("1. Register a new user")
  println("2. Login existing user")
  println("3. Search for a song")

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
          s"ImagePath: ${song.getOrElse("imagePath", "Unknown")}"
      }

      val replyActor = ActorSystem(Behaviors.receiveMessage[List[Map[String, Any]]] { songs =>
        //println(s"ReplyActor received songs: $songs")
        if (songs.nonEmpty) {
          println(s"Number of songs found: ${songs.size}")
          //songs.foreach(song => println(s"Song Info: $song"))
          //songs.foreach(song => println(formatSongInfo(song)))
        } else {
          println("No songs found.")
        }
        Behaviors.stopped
      }, "SearchReplyActor")

      //Route SearchSong command to SongLibraryActor via SystemIntegratorActor
      systemIntegrator ! SystemIntegratorActor.RouteToSongService(
        SearchSong(title, replyActor)
      )

    case _ =>
      println("Invalid choice")
  }
}
