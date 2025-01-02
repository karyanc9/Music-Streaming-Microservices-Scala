package UI

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.scene.paint.Color
import scalafx.scene.text._

object SpotifySignUpPage extends JFXApp {

  stage = new PrimaryStage {
    title = "Spotify - Sign Up"
    scene = new Scene(360, 640) {
      fill = Color.web("#121212") // Background color

      val logoLabel = new Label {
        graphic = new Label {
          text = "\uD83C\uDFB6" // Placeholder for Spotify logo
          style = "-fx-font-size: 40px; -fx-text-fill: #1ED760;"
        }
        alignment = Pos.Center
      }

      val titleLabel = new Label("Sign up to start listening") {
        font = Font.font("Circular Std Bold", FontWeight.Bold, 24)
        textFill = Color.White
        alignment = Pos.Center
      }

      val emailField = new TextField {
        promptText = "Email address"
        style = """-fx-background-color: #121212;
                  -fx-border-color: #535353;
                  -fx-border-width: 1px;
                  -fx-text-fill: white;
                  -fx-font-size: 14px;
                  -fx-padding: 10px;"""
        maxWidth = 300
      }

      val nextButton = new Button("Next") {
        style = """-fx-background-color: #1ED760;
                  -fx-text-fill: black;
                  -fx-font-size: 14px;
                  -fx-padding: 10px 20px;
                  -fx-border-radius: 25px;
                  -fx-background-radius: 25px;"""
        maxWidth = 300
      }

      val separatorLabel = new Label("or") {
        font = Font.font("Arial", FontWeight.Bold, 14)
        textFill = Color.White
        alignment = Pos.Center
      }

      val googleButton = new Button("Sign up with Google") {
        style = """-fx-background-color: transparent;
                  -fx-border-color: #535353;
                  -fx-text-fill: white;
                  -fx-font-size: 14px;
                  -fx-padding: 10px 20px;
                  -fx-border-radius: 25px;
                  -fx-background-radius: 25px;"""
        maxWidth = 300
      }

      val facebookButton = new Button("Sign up with Facebook") {
        style = googleButton.style.value
        maxWidth = 300
      }

      val appleButton = new Button("Sign up with Apple") {
        style = googleButton.style.value
        maxWidth = 300
      }

      val loginLink = new Hyperlink("Already have an account? Log in here.") {
        style = "-fx-text-fill: #1ED760; -fx-font-size: 12px;"
        alignment = Pos.Center
      }

      val privacyText = new Label("This site is protected by reCAPTCHA and the Google Privacy Policy and Terms of Service apply.") {
        style = "-fx-text-fill: #535353; -fx-font-size: 10px; -fx-wrap-text: true;"
        alignment = Pos.Center
        wrapText = true
        maxWidth = 300
      }

      val cardLayout = new VBox(12) {
        padding = Insets(16)
        children = Seq(
          logoLabel,
          titleLabel,
          emailField,
          nextButton,
          separatorLabel,
          googleButton,
          facebookButton,
          appleButton,
          loginLink,
          privacyText
        )
        alignment = Pos.Center
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
