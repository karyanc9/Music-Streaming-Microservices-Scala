package UI

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import actors.UserServiceActor.LoginUser
import main.SongLibraryUI
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout._
import scalafx.scene.paint.Color

object SpotifyLoginUI extends JFXApp {

  // Actor reference for UserServiceActor (Replace with your actual system setup if necessary)
  lazy val userService: ActorSystem[actors.UserServiceActor.Command] = ActorSystem(actors.UserServiceActor(), "UserServiceActor")

  stage = new PrimaryStage {
    title = "Spotify - Log In"
    maximized = true // Maximizes the window without entering full-screen mode
    scene = createLoginScene(this)
  }

  def createLoginScene(stage: PrimaryStage): Scene = {
    new Scene(360, 640) {
      fill = Color.web("#121212") // Dark background color

      // Logo
      val logo = new ImageView {
        image = new Image(getClass.getResourceAsStream("/spotifyLogo.png"))
        fitWidth = 40
        fitHeight = 40
      }

      // Title
      val titleLabel = new Label("Log in to Spotify") {
        style = "-fx-font-family: 'Circular Std'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;"
        alignment = Pos.Center
      }

      // Social Buttons
      def createSocialButton(text: String, logoPath: String): Button = {
        new Button(text) {
          graphic = new ImageView {
            image = new Image(getClass.getResourceAsStream(logoPath))
            fitWidth = 16
            fitHeight = 16
          }
          style = """-fx-background-color: transparent;
                    -fx-border-color: rgba(255, 255, 255, 0.4);
                    -fx-text-fill: white;
                    -fx-font-family: 'Circular Std';
                    -fx-font-size: 14px;
                    -fx-font-weight: bold;
                    -fx-border-radius: 25px;
                    -fx-padding: 10px;"""
          maxWidth = 300
          onMouseEntered = _ => style = style.value.replace("-fx-border-color: rgba(255, 255, 255, 0.4);", "-fx-border-color: white;")
          onMouseExited = _ => style = style.value.replace("-fx-border-color: white;", "-fx-border-color: rgba(255, 255, 255, 0.4);")
        }
      }

      val googleButton = createSocialButton("Continue with Google", "/googleLogo.png")
      val facebookButton = createSocialButton("Continue with Facebook", "/facebookLogo.png")
      val appleButton = createSocialButton("Continue with Apple", "/appleLogo.png")

      // Email Field
      val emailLabel = new Label("Email Address") {
        style = "-fx-font-family: 'Circular Std'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;"
        alignment = Pos.CenterLeft
      }
      val emailField = new TextField {
        promptText = "name@domain.com"
        style = """-fx-background-color: #121212;
                  -fx-border-color: rgba(255, 255, 255, 0.5);
                  -fx-border-radius: 4px;
                  -fx-text-fill: white;
                  -fx-padding: 10px;"""
        maxWidth = 300
        alignment = Pos.CenterLeft
      }

      // Password Field
      val passwordLabel = new Label("Password") {
        style = emailLabel.style.value
        alignment = Pos.CenterLeft
      }
      val passwordField = new PasswordField {
        promptText = "Enter your password"
        style = emailField.style.value
        maxWidth = 300
        alignment = Pos.CenterLeft
      }

      // Feedback Label
      val feedbackLabel = new Label {
        text = ""
        style = "-fx-font-family: 'Circular Std'; -fx-font-size: 12px; -fx-text-fill: white;"
        alignment = Pos.Center
        maxWidth = 300
        wrapText = true
      }

      // Log In Button
      val loginButton = new Button("Log In") {
        style = """-fx-background-color: #1ed760;
                  -fx-text-fill: black;
                  -fx-font-family: 'Circular Std';
                  -fx-font-size: 14px;
                  -fx-font-weight: bold;
                  -fx-border-radius: 50px;
                  -fx-padding: 10px;"""
        maxWidth = 300
        onMouseEntered = _ => style = style.value.replace("#1ed760", "#2af879")
        onMouseExited = _ => style = style.value.replace("#2af879", "#1ed760")
        onAction = _ => {
          val username = emailField.text.value
          val password = passwordField.text.value
          // Validate inputs
          if (username.isEmpty || password.isEmpty) {
            feedbackLabel.text = "Please enter both email and password."
          } else {
            feedbackLabel.text = "Logging in..."
            val replyActor = ActorSystem(Behaviors.receiveMessage[String] { response =>
              if (response == "successful") {
                // Transition to SongLibraryUI

              } else {
                // Display error message directly in the feedback label
                feedbackLabel.text = response
              }
              Behaviors.stopped
            }, "LoginReplyActor")

            userService ! LoginUser(username, password, replyActor)
            stage.scene = SongLibraryUI.createSongLibraryScene()
          }
        }
      }

      // Forgot Password
      val forgotPasswordLink = new Hyperlink("Forgot your password?") {
        style = "-fx-font-family: 'Circular Std'; -fx-font-weight: bold; -fx-text-fill: white; -fx-underline: true;"
        onAction = _ => {
        }
      }

      // Sign Up Link
      val signUpLink = new HBox(5) {
        alignment = Pos.Center
        children = Seq(
          new Label("Don't have an account?") {
            style = "-fx-font-family: 'Circular Std'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: rgba(255, 255, 255, 0.6);"
          },
          new Hyperlink("Sign up for Spotify") {
            style = "-fx-font-family: 'Circular Std'; -fx-font-weight: bold; -fx-text-fill: white; -fx-underline: true;"
            onAction = _ => {
              stage.scene = SpotifySignUpUI.createSignUpScene(stage) // Navigate to Sign-Up Scene
            }
          }
        )
      }

      // Privacy Label
      val privacyLabel = new Label(
        "This site is protected by reCAPTCHA and the Google Privacy Policy and Terms of Service apply."
      ) {
        style = "-fx-font-family: 'Circular Std'; -fx-font-weight: bold; -fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 10px;"
        wrapText = true
        maxWidth = 300
        alignment = Pos.Center
      }

      // Main Layout
      val layout = new VBox(12) {
        padding = Insets(16)
        alignment = Pos.Center
        children = Seq(
          logo,
          titleLabel,
          googleButton,
          facebookButton,
          appleButton,
          emailLabel,
          emailField,
          passwordLabel,
          passwordField,
          loginButton,
          feedbackLabel,
          forgotPasswordLink,
          signUpLink,
          privacyLabel
        )
      }

      root = new StackPane {
        children = Seq(layout)
        style = "-fx-background-color: #121212;"
      }
    }
  }
}