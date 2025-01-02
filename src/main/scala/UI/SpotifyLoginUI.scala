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
        font = Font.font("SpotifyMixUITitle", FontWeight.Bold, 20)
        textFill = Color.web("#ffffff")
        alignment = Pos.Center
      }

      val googleButton = new Button("Continue with Google") {
        style = """-fx-background-color: transparent;
                  -fx-border-color: #ffffff;
                  -fx-border-width: 1px;
                  -fx-text-fill: #ffffff;
                  -fx-font-size: 14px;
                  -fx-padding: 8px 32px;
                  -fx-border-radius: 24px;
                  -fx-background-radius: 24px;"""
        maxWidth = 300
      }

      val facebookButton = new Button("Continue with Facebook") {
        style = """-fx-background-color: transparent;
                  -fx-border-color: #ffffff;
                  -fx-border-width: 1px;
                  -fx-text-fill: #ffffff;
                  -fx-font-size: 14px;
                  -fx-padding: 8px 32px;
                  -fx-border-radius: 24px;
                  -fx-background-radius: 24px;"""
        maxWidth = 300
      }

      val appleButton = new Button("Continue with Apple") {
        style = """-fx-background-color: transparent;
                  -fx-border-color: #ffffff;
                  -fx-border-width: 1px;
                  -fx-text-fill: #ffffff;
                  -fx-font-size: 14px;
                  -fx-padding: 8px 32px;
                  -fx-border-radius: 24px;
                  -fx-background-radius: 24px;"""
        maxWidth = 300
      }

      val emailField = new TextField {
        promptText = "Email or username"
        style = """-fx-background-color: #121212;
                  -fx-text-fill: #ffffff;
                  -fx-font-size: 14px;
                  -fx-border-color: #7c7c7c;
                  -fx-border-width: 1px;
                  -fx-padding: 12px;"""
        maxWidth = 300
      }

      val passwordField = new PasswordField {
        promptText = "Password"
        style = emailField.style.value
        maxWidth = 300
      }

      val loginButton = new Button("Log In") {
        style = """-fx-background-color: #1ed760;
                  -fx-text-fill: #ffffff;
                  -fx-font-size: 14px;
                  -fx-padding: 8px;
                  -fx-border-radius: 24px;
                  -fx-background-radius: 24px;"""
        maxWidth = 300
      }

      val forgotPasswordLink = new Hyperlink("Forgot your password?") {
        style = "-fx-text-fill: #ffffff; -fx-font-size: 12px;"
      }

      val signupLink = new Hyperlink("Sign up for Spotify") {
        style = "-fx-text-fill: #1ed760; -fx-font-size: 12px;"
      }

      val separator = new Separator {
        style = "-fx-background-color: #7c7c7c;"
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
            style = "-fx-text-fill: #b3b3b3; -fx-font-size: 12px;"
          },
          signupLink
        )
        alignment = Pos.Center
        style = """-fx-background-color: #1f1f1f;
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
