package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import utils.FirebaseUtils


object UserServiceActor {
  sealed trait Command
  case class RegisterUser(username: String, password: String, replyTo: ActorRef[String]) extends Command
  case class LoginUser(username: String, password: String, replyTo: ActorRef[String]) extends Command

  def apply(): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case RegisterUser(username, password, replyTo) =>
        context.log.info(s"Registering user: $username")
        val response = FirebaseUtils.registerUser(username, password)
        replyTo ! response.getOrElse("Registration failed!")
        Behaviors.same

      case LoginUser(username, password, replyTo) =>
        context.log.info(s"Logging in user: $username")
        val response = FirebaseUtils.loginUser(username, password)
        replyTo ! response.getOrElse("Login failed!")
        Behaviors.same
    }
  }
}
