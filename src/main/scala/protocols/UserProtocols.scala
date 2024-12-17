package protocols

object UserProtocols {
  sealed trait Command
  case class RegisterUser(username: String, password: String) extends Command
  case class LoginUser(username: String, password: String) extends Command
}
