package protocols

// Define messages for user-related operations
object UserProtocols {

  // Registration-related messages
  case class RegisterUser(username: String, password: String)
  case class RegisterResponse(success: Boolean, message: String)

  // Login-related messages
  case class LoginUser(username: String, password: String)
  case class LoginResponse(success: Boolean, message: String, token: Option[String] = None)

  // Session validation messages
  case class ValidateSession(token: String)
  case class ValidateSessionResponse(valid: Boolean, message: String)
}
