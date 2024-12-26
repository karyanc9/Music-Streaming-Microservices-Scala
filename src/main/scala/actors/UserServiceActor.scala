package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import utils.FirebaseUtils
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object UserServiceActor {
  // Define the commands for user operations
  sealed trait Command
  case class RegisterUser(username: String, password: String, replyTo: ActorRef[String]) extends Command
  case class LoginUser(username: String, password: String, replyTo: ActorRef[String]) extends Command
  case class ValidateSession(token: String, replyTo: ActorRef[Boolean]) extends Command

  // Define the behavior of the actor
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    context.log.info("UserServiceActor initialized")

    Behaviors.receiveMessage {
      case RegisterUser(username, password, replyTo) =>
        context.log.info(s"Registering user: $username")
        FirebaseUtils.registerUser(username, password).onComplete {
          case Success(Some(message)) =>
            replyTo ! message
          case Success(None) =>
            replyTo ! "Registration failed!"
          case Failure(exception) =>
            context.log.error(s"Error during registration: ${exception.getMessage}")
            replyTo ! "Registration failed due to an error!"
        }
        Behaviors.same

      case LoginUser(username, password, replyTo) =>
        context.log.info(s"Logging in user: $username")
        FirebaseUtils.loginUser(username, password).onComplete {
          case Success(Some(message)) =>
            FirebaseUtils.createSession(username).onComplete {
              case Success(token) =>
                replyTo ! s"$message Session Token: $token"
              case Failure(exception) =>
                replyTo ! s"$message, but session creation failed: ${exception.getMessage}"
            }
          case Success(None) =>
            replyTo ! "Login failed!"
          case Failure(exception) =>
            context.log.error(s"Error during login: ${exception.getMessage}")
            replyTo ! "Login failed due to an error!"
        }
        Behaviors.same

      case ValidateSession(token, replyTo) =>
        FirebaseUtils.validateSession(token).onComplete {
          case Success(isValid) =>
            replyTo ! isValid
          case Failure(exception) =>
            context.log.error(s"Error validating session: ${exception.getMessage}")
            replyTo ! false
        }
        Behaviors.same
    }
  }
}
