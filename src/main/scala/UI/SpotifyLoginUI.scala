package UI

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.scene.paint._
import scalafx.scene.text._

object SpotifyLoginUI extends JFXApp {

  stage = new PrimaryStage {
    title = "Spotify - Login"
    scene = new Scene(360, 640) {
      fill = Color.web("#121212")

      val logoLabel = new Label {
        graphic = new Label {
          text = "\uD83C\uDFB6" // Spotify logo placeholder
          style = "-fx-font-size: 40px; -fx-text-fill: #1ed760;"
        }
        alignment = Pos.Center
      }

      val titleLabel = new Label("Log in to Spotify") {
        font = Font.font("Circular Std Bold", FontWeight.Bold, 24)
        textFill = Color.web("#ffffff")
        alignment = Pos.Center
      }

      val googleButton = new Button("Continue with Google") {
        style = """-fx-background-color: transparent;
                  -fx-border-color: rgba(255, 255, 255, 0.4);
                  -fx-text-fill: #ffffff;
                  -fx-font-size: 14px;
                  -fx-border-radius: 25px;
                  -fx-padding: 11px 10px;"""
        maxWidth = 300
      }

      val facebookButton = new Button("Continue with Facebook") {
        style = googleButton.style.value
        maxWidth = 300
      }

      val appleButton = new Button("Continue with Apple") {
        style = googleButton.style.value
        maxWidth = 300
      }

      val emailField = new TextField {
        promptText = "Email or username"
        style = """-fx-background-color: #121212;
                  -fx-text-fill: rgba(255, 255, 255, 0.5);
                  -fx-font-size: 14px;
                  -fx-border-color: rgba(255, 255, 255, 0.5);
                  -fx-border-width: 1px;
                  -fx-padding: 11.5px;"""
        maxWidth = 300
      }

      val passwordField = new PasswordField {
        promptText = "Password"
        style = emailField.style.value
        maxWidth = 300
      }

      val loginButton = new Button("Log In") {
        style = """-fx-background-color: rgba(30, 215, 96, 1);
                  -fx-text-fill: black;
                  -fx-font-family: 'Circular Std Bold';
                  -fx-border-radius: 25px;
                  -fx-padding: 11px;"""
        maxWidth = 300
      }

      val forgotPasswordLink = new Hyperlink("Forgot your password?") {
        style = "-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 12px;"
      }

      val signupLink = new Hyperlink("Sign up for Spotify") {
        style = "-fx-text-fill: rgba(30, 215, 96, 1); -fx-font-size: 12px;"
      }

      val separator = new Separator {
        style = "-fx-background-color: rgba(255, 255, 255, 0.6);"
      }

      val cardLayout = new VBox(12) {
        padding = Insets(24, 16, 24, 16)
        children = Seq(
          logoLabel,
          titleLabel,
          googleButton,
          facebookButton,
          appleButton,
          separator,
          emailField,
          passwordField,
          loginButton,
          forgotPasswordLink,
          new Label("Don't have an account?") {
            style = "-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 12px;"
          },
          signupLink
        )
        alignment = Pos.Center
        style = """-fx-background-color: black;
                  -fx-border-radius: 8px;
                  -fx-background-radius: 8px;"""
      }

      val rootLayout = new VBox {
        alignment = Pos.Center
        padding = Insets(32)
        children = Seq(cardLayout)
      }

      root = rootLayout
    }
  }
}
