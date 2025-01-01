package UI

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.geometry.Insets
import scalafx.scene.text.Font
import scalafx.scene.paint.Color

object SpotifyLoginUI extends JFXApp {

  stage = new PrimaryStage {
    title = "Spotify - Login"
    scene = new Scene(400, 600) {
      fill = Color.Black

      val titleLabel = new Label("Log in to Spotify") {
        style = "-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: white;"
      }

      val googleButton = new Button("Continue with Google") {
        style = "-fx-background-color: #ffffff; -fx-text-fill: #000000; -fx-padding: 10px;"
        maxWidth = Double.MaxValue
      }

      val facebookButton = new Button("Continue with Facebook") {
        style = "-fx-background-color: #3b5998; -fx-text-fill: #ffffff; -fx-padding: 10px;"
        maxWidth = Double.MaxValue
      }

      val appleButton = new Button("Continue with Apple") {
        style = "-fx-background-color: #000000; -fx-text-fill: #ffffff; -fx-padding: 10px;"
        maxWidth = Double.MaxValue
      }

      val emailLabel = new Label("Email or username") {
        style = "-fx-text-fill: white;"
      }
      val emailField = new TextField {
        promptText = "Email or username"
        maxWidth = Double.MaxValue
      }

      val passwordLabel = new Label("Password") {
        style = "-fx-text-fill: white;"
      }
      val passwordField = new PasswordField {
        promptText = "Password"
        maxWidth = Double.MaxValue
      }

      val loginButton = new Button("Log In") {
        style = "-fx-background-color: #1db954; -fx-text-fill: white; -fx-font-size: 14;"
        maxWidth = Double.MaxValue
      }

      val forgotPasswordLink = new Hyperlink("Forgot your password?") {
        style = "-fx-text-fill: white;"
      }

      val vbox = new VBox(15) {
        padding = Insets(20)
        children = Seq(
          titleLabel,
          googleButton,
          facebookButton,
          appleButton,
          new Separator { style = "-fx-background-color: white;" },
          emailLabel,
          emailField,
          passwordLabel,
          passwordField,
          loginButton,
          forgotPasswordLink
        )
        style = "-fx-background-color: #000000;"
      }

      root = vbox
    }
  }
}
