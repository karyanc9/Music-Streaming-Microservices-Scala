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
      fill = Color.web("#121212") // Dark background color for the entire page

      // Logo
      val logoLabel = new Label {
        graphic = new Label {
          text = "\uD83C\uDFB6" // Placeholder for Spotify logo
          style = "-fx-font-size: 40px; -fx-text-fill: #1ed760;"
        }
        alignment = Pos.Center
      }

      // Title
      val titleLabel = new Label {
        text = "Sign up to\nstart listening"
        font = Font.font("Circular Std Bold", FontWeight.Bold, 24)
        textFill = Color.web("#ffffff")
        alignment = Pos.Center
        style = "-fx-text-alignment: center;"
      }

      // Email Address Label
      val emailLabel = new Label("Email address") {
        textFill = Color.web("#ffffff")
        font = Font.font("Circular Std Medium", FontWeight.Normal, 12)
        alignment = Pos.CenterLeft
      }

      // Email Address Input
      val emailField = new TextField {
        promptText = "name@domain.com"
        style = """-fx-background-color: #121212;
                  -fx-text-fill: #ffffff;
                  -fx-font-size: 14px;
                  -fx-border-color: rgba(255, 255, 255, 0.5);
                  -fx-border-radius: 4px;
                  -fx-padding: 10px;"""
        maxWidth = 300
      }

      // Next Button
      val nextButton = new Button("Next") {
        style = """-fx-background-color: #1ed760;
                  -fx-text-fill: black;
                  -fx-font-size: 14px;
                  -fx-font-family: 'Circular Std Bold';
                  -fx-border-radius: 25px;
                  -fx-padding: 10px;"""
        maxWidth = 300
      }

      // OR Separator
      val separatorLabel = new Label("or") {
        textFill = Color.web("#ffffff")
        font = Font.font("Circular Std Medium", FontWeight.Normal, 12)
        alignment = Pos.Center
      }

      val separator = new Separator {
        style = "-fx-background-color: rgba(255, 255, 255, 0.5);"
      }

      // Social Buttons
      val googleButton = new Button("Sign up with Google") {
        style = """-fx-background-color: transparent;
                  -fx-border-color: rgba(255, 255, 255, 0.4);
                  -fx-text-fill: #ffffff;
                  -fx-font-size: 14px;
                  -fx-border-radius: 25px;
                  -fx-padding: 10px;"""
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

      // Already Have Account Link
      val loginLink = new Hyperlink("Already have an account? Log in here.") {
        style = "-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 12px;"
      }

      // Privacy Policy
      val privacyLabel = new Label(
        "This site is protected by reCAPTCHA and the Google Privacy Policy and Terms of Service apply."
      ) {
        textFill = Color.web("#ffffff")
        font = Font.font("Circular Std Light", FontWeight.Light, 10)
        wrapText = true
        alignment = Pos.Center
        maxWidth = 300
      }

      // Layout
      val layout = new VBox(10) {
        padding = Insets(16)
        children = Seq(
          logoLabel,
          titleLabel,
          emailLabel,
          emailField,
          nextButton,
          separatorLabel,
          googleButton,
          facebookButton,
          appleButton,
          loginLink,
          privacyLabel
        )
        alignment = Pos.TopCenter
        style = "-fx-background-color: #121212;" // Ensures the layout background matches the scene background
      }

      root = new StackPane {
        children = Seq(layout)
        style = "-fx-background-color: #121212;" // Ensures no light areas remain
      }
    }
  }
}
