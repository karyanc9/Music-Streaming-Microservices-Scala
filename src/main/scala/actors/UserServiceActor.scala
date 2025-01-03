package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import utils.FirebaseUtils
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object UserServiceActor {
  sealed trait Command
  case class RegisterUser(username: String, password: String, replyTo: ActorRef[String]) extends Command
  case class LoginUser(username: String, password: String, replyTo: ActorRef[String]) extends Command
  case class ValidateSession(token: String, replyTo: ActorRef[Boolean]) extends Command
  case class DeleteUser(username: String, replyTo: ActorRef[String]) extends Command
  case class UpdatePassword(username: String, newPassword: String, replyTo: ActorRef[String]) extends Command
  case class WrappedResultString(result: Either[Throwable, String], replyTo: ActorRef[String]) extends Command
  case class WrappedResultBoolean(result: Either[Throwable, Boolean], replyTo: ActorRef[Boolean]) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    context.log.info("UserServiceActor initialized")

    Behaviors.receiveMessage {
      case RegisterUser(username, password, replyTo) =>
        context.log.info(s"Received RegisterUser command for username: $username") // Log the command
        context.pipeToSelf(FirebaseUtils.registerUser(username, password)) {
          case Success(Some(message)) =>
            context.log.info(s"Registration succeeded: $message") // Log success
            WrappedResultString(Right(message), replyTo)
          case Success(None) =>
            context.log.warn("Registration failed: No response from Firebase") // Use `warn` instead of `warning`
            WrappedResultString(Right("Registration failed. Please try again."), replyTo)
          case Failure(exception) =>
            context.log.error(s"Registration failed with exception: ${exception.getMessage}") // Log exception
            WrappedResultString(Left(exception), replyTo)
        }
        Behaviors.same

      case LoginUser(username, password, replyTo) =>
        context.pipeToSelf(FirebaseUtils.loginUser(username, password)) {
          case Success(Some(message)) => WrappedResultString(Right(message), replyTo)
          case Success(None)          => WrappedResultString(Right("Login failed."), replyTo)
          case Failure(exception)     => WrappedResultString(Left(exception), replyTo)
        }
        Behaviors.same

      case ValidateSession(token, replyTo) =>
        context.pipeToSelf(FirebaseUtils.validateSession(token)) {
          case Success(isValid) => WrappedResultBoolean(Right(isValid), replyTo)
          case Failure(exception) => WrappedResultBoolean(Left(exception), replyTo)
        }
        Behaviors.same

      case WrappedResultString(result, replyTo) =>
        result match {
          case Right(response) =>
            context.log.info(s"Operation succeeded with response: $response") // Log success
            replyTo ! response // Send the success message back to the UI
          case Left(exception) =>
            val errorMessage = s"Error: ${exception.getMessage}"
            context.log.error(s"Operation failed with error: $errorMessage") // Log failure
            replyTo ! errorMessage // Send the error message back to the UI
        }
        Behaviors.same

      case WrappedResultBoolean(result, replyTo) =>
        result match {
          case Right(response) =>
            replyTo ! response
          case Left(exception) =>
            context.log.error(s"Operation failed: ${exception.getMessage}")
            replyTo ! false
        }
        Behaviors.same
    }
  }
}
