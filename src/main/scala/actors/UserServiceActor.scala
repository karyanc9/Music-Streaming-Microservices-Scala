package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import utils.FirebaseUtils
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object UserServiceActor {

  // Define supported commands for the UserServiceActor
  sealed trait Command
  case class RegisterUser(username: String, password: String, replyTo: ActorRef[String]) extends Command
  case class LoginUser(username: String, password: String, replyTo: ActorRef[String]) extends Command
  case class ValidateSession(token: String, replyTo: ActorRef[Boolean]) extends Command
  case class DeleteUser(username: String, replyTo: ActorRef[String]) extends Command
  case class UpdatePassword(username: String, newPassword: String, replyTo: ActorRef[String]) extends Command
  case class WrappedResultString(result: Either[Throwable, String], replyTo: ActorRef[String]) extends Command
  case class WrappedResultBoolean(result: Either[Throwable, Boolean], replyTo: ActorRef[Boolean]) extends Command

  // Define the behavior
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    context.log.info("UserServiceActor initialized.")

    Behaviors.receiveMessage {
      // Handle user registration
      case RegisterUser(username, password, replyTo) =>
        context.log.info(s"Processing RegisterUser for username: $username.")
        context.pipeToSelf(FirebaseUtils.registerUser(username, password)) {
          case Success(Some(message)) =>
            context.log.info(s"User registered successfully: $message.")
            WrappedResultString(Right(message), replyTo)
          case Success(None) =>
            context.log.warn("Registration failed: Firebase returned no response.")
            WrappedResultString(Right("Registration failed. Please try again."), replyTo)
          case Failure(exception) =>
            context.log.error(s"Registration failed with exception: ${exception.getMessage}")
            WrappedResultString(Left(exception), replyTo)
        }
        Behaviors.same

      // Handle user login
      case LoginUser(username, password, replyTo) =>
        context.log.info(s"Processing LoginUser for username: $username.")
        context.pipeToSelf(FirebaseUtils.loginUser(username, password)) {
          case Success(Some(message)) =>
            context.log.info(s"Login successful for user: $username.")
            WrappedResultString(Right(message), replyTo)
          case Success(None) =>
            context.log.warn(s"Login failed for user: $username. Incorrect credentials.")
            WrappedResultString(Right("Login failed. Please check your username and password."), replyTo)
          case Failure(exception) =>
            context.log.error(s"Login failed with exception: ${exception.getMessage}")
            WrappedResultString(Left(exception), replyTo)
        }
        Behaviors.same

      // Handle session validation
      case ValidateSession(token, replyTo) =>
        context.log.info(s"Validating session for token: $token.")
        context.pipeToSelf(FirebaseUtils.validateSession(token)) {
          case Success(isValid) =>
            context.log.info(s"Session validation result: $isValid.")
            WrappedResultBoolean(Right(isValid), replyTo)
          case Failure(exception) =>
            context.log.error(s"Session validation failed with exception: ${exception.getMessage}")
            WrappedResultBoolean(Left(exception), replyTo)
        }
        Behaviors.same



      // Handle wrapped results for string responses
      case WrappedResultString(result, replyTo) =>
        result match {
          case Right(response) =>
            context.log.info(s"Operation completed successfully with response: $response.")
            replyTo ! response
          case Left(exception) =>
            val errorMessage = s"Error: ${exception.getMessage}"
            context.log.error(s"Operation failed with error: $errorMessage.")
            replyTo ! errorMessage
        }
        Behaviors.same

      // Handle wrapped results for boolean responses
      case WrappedResultBoolean(result, replyTo) =>
        result match {
          case Right(isValid) =>
            context.log.info(s"Boolean operation result: $isValid.")
            replyTo ! isValid
          case Left(exception) =>
            context.log.error(s"Boolean operation failed with exception: ${exception.getMessage}")
            replyTo ! false
        }
        Behaviors.same
    }
  }
}