package main

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import actors.{MusicPlayerActor, SongLibraryActor, SystemIntegratorActor, UserServiceActor}
import protocols.SongProtocols
import actors.UserServiceActor.{LoginUser, RegisterUser, ValidateSession}
import utils.FirebaseUtils
import akka.actor.typed.ActorRef

object Main extends App {
  // Initialize Firebase
  FirebaseUtils.initializeFirebase()

  // Create Actors
  val userService: ActorRef[UserServiceActor.Command] = ActorSystem(UserServiceActor(), "UserServiceActor")
  val songLibrary: ActorRef[SongProtocols.Command] = ActorSystem(SongLibraryActor(), "SongLibraryActor")
  val musicPlayer: ActorRef[SongProtocols.Command] = ActorSystem(MusicPlayerActor(), "MusicPlayerActor")

  // Wrap songLibrary and musicPlayer using SystemIntegratorActor
  val systemIntegrator: ActorRef[SystemIntegratorActor.Command] = ActorSystem(
    SystemIntegratorActor(userService, songLibrary, musicPlayer),
    "SystemIntegratorActor"
  )

  // Interactive Menu
  println("Welcome to Spotify Distributed System")
  println("1. Register a new user")
  println("2. Login existing user")
  println("3. Search for a song")
  println("4. Validate session token")

  val choice = scala.io.StdIn.readInt()

  choice match {
    case 1 =>
      println("Enter username (email):")
      val username = scala.io.StdIn.readLine()
      println("Enter password:")
      val password = scala.io.StdIn.readLine()

      val replyActor = ActorSystem(Behaviors.receiveMessage[String] { response =>
        println(response)
        Behaviors.stopped
      }, "RegisterReplyActor")

      userService ! RegisterUser(username, password, replyActor)

    case 2 =>
      println("Enter username (email):")
      val username = scala.io.StdIn.readLine()
      println("Enter password:")
      val password = scala.io.StdIn.readLine()

      val replyActor = ActorSystem(Behaviors.receiveMessage[String] { response =>
        println(response)
        Behaviors.stopped
      }, "LoginReplyActor")

      userService ! LoginUser(username, password, replyActor)

    case 3 =>
      println("Enter song title to search:")
      val title = scala.io.StdIn.readLine()

      val replyActor = ActorSystem(Behaviors.receiveMessage[List[Map[String, Any]]] { songs =>
        if (songs.nonEmpty) {
          songs.foreach(song => println(s"Song Info: $song"))
        } else {
          println("No songs found.")
        }
        Behaviors.stopped
      }, "SearchReplyActor")

      systemIntegrator ! SystemIntegratorActor.RouteToSongService(SongProtocols.SearchSong(title, replyActor))

    case 4 =>
      println("Enter session token to validate:")
      val token = scala.io.StdIn.readLine()

      val replyActor = ActorSystem(Behaviors.receiveMessage[Boolean] { isValid =>
        if (isValid) println("Session token is valid.") else println("Invalid session token.")
        Behaviors.stopped
      }, "ValidateSessionReplyActor")

      userService ! ValidateSession(token, replyActor)

    case _ =>
      println("Invalid choice")
  }
}
