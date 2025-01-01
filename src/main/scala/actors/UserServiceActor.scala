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

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    context.log.info("UserServiceActor initialized")

    Behaviors.receiveMessage {
      case RegisterUser(username, password, replyTo) =>
        FirebaseUtils.registerUser(username, password).onComplete {
          case Success(Some(message)) => replyTo ! message
          case _                      => replyTo ! "Registration failed."
        }
        Behaviors.same

      case LoginUser(username, password, replyTo) =>
        FirebaseUtils.loginUser(username, password).onComplete {
          case Success(Some(message)) =>
            FirebaseUtils.checkExistingSession(username).onComplete {
              case Success(Some(token)) =>
                replyTo ! s"$message Existing Session Token: $token"
              case _ =>
                FirebaseUtils.createSession(username).onComplete {
                  case Success(token) => replyTo ! s"$message New Session Token: $token"
                  case Failure(e)     => replyTo ! s"$message, but session creation failed: ${e.getMessage}"
                }
            }
          case _ => replyTo ! "Login failed."
        }
        Behaviors.same

      case ValidateSession(token, replyTo) =>
        FirebaseUtils.validateSession(token).onComplete {
          case Success(isValid) => replyTo ! isValid
          case _                => replyTo ! false
        }
        Behaviors.same
    }
  }
}
