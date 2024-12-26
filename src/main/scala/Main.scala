package main

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import actors.UserServiceActor
import actors.UserServiceActor.{RegisterUser, LoginUser}
import utils.FirebaseUtils

import akka.actor.typed.ActorRef

object Main extends App {
  // Initialize Firebase
  FirebaseUtils.initializeFirebase()

  // Initialize UserServiceActor - Create the main ActorSystem once
  val userService: ActorRef[UserServiceActor.Command] = ActorSystem(UserServiceActor(), "UserServiceActor")

  // Interactive Menu
  println("Welcome to Spotify Distributed System")
  println("1. Register a new user")
  println("2. Login existing user")

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

    case _ =>
      println("Invalid choice")
  }
}
