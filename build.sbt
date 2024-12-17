ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.15"

// Add ScalaFX
libraryDependencies += "org.scalafx" %% "scalafx" % "23.0.1-R34"

// Add JavaFX
val javaFxVersion = "15.0.1"
libraryDependencies ++= Seq(
  "org.openjfx" % "javafx-base" % javaFxVersion classifier "win",
  "org.openjfx" % "javafx-controls" % javaFxVersion classifier "win",
  "org.openjfx" % "javafx-media" % javaFxVersion classifier "win"
)

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/"

Compile / run / javaOptions ++= Seq(
  "--module-path", sys.props("user.home") + "/.ivy2/cache/org.openjfx",
  "--add-modules", "javafx.controls,javafx.media"
)

lazy val root = (project in file("."))
  .settings(
    name := "DS_A3"
  )
