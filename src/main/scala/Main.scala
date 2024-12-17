package main

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import actors.{SystemIntegratorActor, UserServiceActor}
import akka.actor.typed.ActorRef
import utils.FirebaseUtils

object Main extends App {
  // Initialize Firebase
  FirebaseUtils.initializeFirebase()

  // Initialize Actor System
  val userService: ActorRef[UserServiceActor.Command] = ActorSystem(UserServiceActor(), "UserServiceActor")
  val systemIntegrator: ActorRef[SystemIntegratorActor.Command] = ActorSystem(
    SystemIntegratorActor(userService, null, null), // Replace null with other services when implemented
    "SystemIntegratorActor"
  )

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

      val replyActor = ActorSystem(Behaviors.receiveMessage[String] { response =>
        println(response)
        Behaviors.stopped // Stop the reply actor after receiving the response
      }, "RegisterReplyActor")

      systemIntegrator ! SystemIntegratorActor.RouteToUserService(
        UserServiceActor.RegisterUser(username, password, replyActor),
        replyActor
      )

    case 2 =>
      println("Enter username (email):")
      val username = scala.io.StdIn.readLine()
      println("Enter password:")
      val password = scala.io.StdIn.readLine()

      val replyActor = ActorSystem(Behaviors.receiveMessage[String] { response =>
        println(response)
        Behaviors.stopped // Stop the reply actor after receiving the response
      }, "LoginReplyActor")

      systemIntegrator ! SystemIntegratorActor.RouteToUserService(
        UserServiceActor.LoginUser(username, password, replyActor),
        replyActor
      )

    case _ =>
      println("Invalid choice")
  }
}
