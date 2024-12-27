package utils

import utils.FirebaseUtils
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object TestFirebaseUtils {
  def main(args: Array[String]): Unit = {
    // Initialize Firebase
    FirebaseUtils.initializeFirebase()

    // Test searchSong method
    val testSongTitle = "Momma Mia" // Replace with an existing or non-existing title
    FirebaseUtils.searchSong(testSongTitle).onComplete {
      case Success(songs) =>
        println(s"Standalone Test: Found songs: $songs")
      case Failure(exception) =>
        println(s"Standalone Test: Error occurred: ${exception.getMessage}")
    }

    // Keep the main thread alive for the async operation to complete
    Thread.sleep(5000)
  }
}
