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
        context.pipeToSelf(FirebaseUtils.registerUser(username, password)) {
          case Success(Some(message)) => WrappedResultString(Right(message), replyTo)
          case Success(None)          => WrappedResultString(Right("Registration failed."), replyTo)
          case Failure(exception)     => WrappedResultString(Left(exception), replyTo)
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
            replyTo ! response
          case Left(exception) =>
            context.log.error(s"Operation failed: ${exception.getMessage}")
            replyTo ! s"Error: ${exception.getMessage}"
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
