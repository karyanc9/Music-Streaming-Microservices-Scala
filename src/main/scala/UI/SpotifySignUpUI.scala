package UI

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import actors.UserServiceActor.RegisterUser
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout._
import scalafx.scene.paint.Color

object SpotifySignUpUI extends JFXApp {

  // Actor reference for UserServiceActor
  val userService: ActorSystem[actors.UserServiceActor.Command] = ActorSystem(actors.UserServiceActor(), "UserServiceActor")

  stage = new PrimaryStage {
    title = "Spotify - Sign Up"
    maximized = true
    scene = createSignUpScene(this)
  }

  def createSignUpScene(stage: PrimaryStage): Scene = {
    new Scene(360, 640) {
      fill = Color.web("#121212") // Dark background color

      // Logo
      val logoLabel = new ImageView {
        image = new Image(getClass.getResourceAsStream("/spotifyLogo.png"))
        fitWidth = 40
        fitHeight = 40
      }

      // Title
      val titleLabel = new Label {
        text = "Sign up to\nstart listening"
        style = "-fx-font-family: 'Circular Std'; -fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-text-alignment: center;"
        alignment = Pos.Center
      }

      // Email Label
      val emailLabel = new Label("Email address") {
        style = "-fx-font-family: 'Circular Std'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffffff;"
        alignment = Pos.CenterLeft
        maxWidth = 300
      }

      // Email Input
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

      // Password Label
      val passwordLabel = new Label("Password") {
        style = "-fx-font-family: 'Circular Std'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffffff;"
        alignment = Pos.CenterLeft
        maxWidth = 300
      }

      // Password Input
      val passwordField = new PasswordField {
        promptText = "Enter your password"
        style = """-fx-background-color: #121212;
                  -fx-text-fill: #ffffff;
                  -fx-font-size: 14px;
                  -fx-border-color: rgba(255, 255, 255, 0.5);
                  -fx-border-radius: 4px;
                  -fx-padding: 10px;"""
        maxWidth = 300
      }

      // Feedback Label
      val feedbackLabel = new Label {
        text = ""
        style = "-fx-font-family: 'Circular Std'; -fx-font-size: 12px; -fx-text-fill: white;"
        alignment = Pos.Center
        maxWidth = 300
        wrapText = true
      }

      // Next Button
      val nextButton = new Button("Next") {
        style =
          """-fx-background-color: #1ed760;
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
            feedbackLabel.text = "Signing up..."
            disable = true // Disable the button during sign-up

            // Temporary actor to handle responses
            val replyActor = ActorSystem(Behaviors.receiveMessage[String] { response =>
              println(s"Response received: $response") // Debugging log
              scalafx.application.Platform.runLater {
                feedbackLabel.text = response // Update feedback label
                disable = false // Re-enable the button after response
              }
              Behaviors.stopped
            }, "SignUpReplyActor")

            // Send RegisterUser command to UserServiceActor
            userService ! RegisterUser(username, password, replyActor)
          }
        }
      }

      // OR Separator
      val separatorBox = new HBox(5) {
        alignment = Pos.Center
        children = Seq(
          new Separator { maxWidth = 180; style = "-fx-background-color: rgba(255, 255, 255, 0.2);" },
          new Label("or") {
            style = "-fx-font-family: 'Circular Std'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #ffffff;"
          },
          new Separator { maxWidth = 180; style = "-fx-background-color: rgba(255, 255, 255, 0.2);" }
        )
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

      val googleButton = createSocialButton("Sign up with Google", "/googleLogo.png")
      val facebookButton = createSocialButton("Sign up with Facebook", "/facebookLogo.png")
      val appleButton = createSocialButton("Sign up with Apple", "/appleLogo.png")

      // Already Have Account Link
      val loginLink = new Hyperlink("Log in here") {
        style = "-fx-font-family: 'Circular Std'; -fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-font-weight: bold; -fx-underline: true;"
        onAction = _ => {
          stage.scene = SpotifyLoginUI.createLoginScene(stage) // Redirect to Login Page
        }
      }

      val alreadyHaveAccountLabel = new HBox(5) {
        alignment = Pos.Center
        children = Seq(
          new Label("Already have an account?") {
            style = "-fx-font-family: 'Circular Std'; -fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 14px;"
          },
          loginLink
        )
      }

      // Privacy Policy
      val privacyLabel = new Label(
        "This site is protected by reCAPTCHA and the Google Privacy Policy and Terms of Service apply."
      ) {
        style = "-fx-font-family: 'Circular Std'; -fx-text-fill: #ffffff; -fx-font-size: 10px; -fx-alignment: center;"
        wrapText = true
        alignment = Pos.Center
        maxWidth = 300
      }

      // Main Layout
      val layout = new VBox(12) {
        padding = Insets(16)
        children = Seq(
          logoLabel,
          titleLabel,
          emailLabel,
          emailField,
          passwordLabel,
          passwordField,
          nextButton,
          feedbackLabel,
          separatorBox,
          googleButton,
          facebookButton,
          appleButton,
          alreadyHaveAccountLabel,
          privacyLabel
        )
        alignment = Pos.Center
        style = "-fx-background-color: #121212;"
        minHeight = 640
      }

      root = new StackPane {
        children = Seq(layout)
        style = "-fx-background-color: #121212;"
      }
    }
  }
}
