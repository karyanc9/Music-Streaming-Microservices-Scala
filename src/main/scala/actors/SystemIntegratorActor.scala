package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object SystemIntegratorActor {
  sealed trait Command
  case class RouteToUserService(msg: UserServiceActor.Command, replyTo: ActorRef[String]) extends Command

  def apply(
             userService: ActorRef[UserServiceActor.Command],
             songService: ActorRef[Nothing],
             playlistService: ActorRef[Nothing]
           ): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case RouteToUserService(msg, replyTo) =>
          userService ! msg
          replyTo ! "Message routed to UserService"
          Behaviors.same
      }
    }
  }
}
