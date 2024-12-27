package utils

import utils.FirebaseUtils
import scala.util.{Success, Failure}
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object SongMetadataUploader {
  def uploadSongsMetadata(): Future[Unit] = {
    val songs = Map(
      "song1" -> Map(
        "title" -> "Mamma Mia",
        "artist" -> "ABBA",
        "genre" -> "Pop",
        "duration" -> "3:30",
        "filePath" -> "songs/song1.mp3",
        "imagePath" -> "songs/song1.jpg"
      ),
      "song2" -> Map(
        "title" -> "Billie Jean",
        "artist" -> "Michael Jackson",
        "genre" -> "Disco",
        "duration" -> "4:45",
        "filePath" -> "songs/song2.mp3",
        "imagePath" -> "songs/song2.jpg"
      ),
      "song3" -> Map(
        "title" -> "Bohemian Rhapsody",
        "artist" -> "Queen",
        "genre" -> "Hard rock",
        "duration" -> "5:59",
        "filePath" -> "songs/song3.mp3",
        "imagePath" -> "songs/song3.jpg"
      )
    )

    // Collect all Futures into a single Future
    val uploadFutures = songs.map { case (songId, metadata) =>
      FirebaseUtils.saveSongMetadata(songId, metadata).andThen {
        case Success(_) => println(s"Successfully uploaded metadata for song: $songId")
        case Failure(exception) => println(s"Failed to upload metadata for song: $songId. Error: ${exception.getMessage}")
      }
    }

    Future.sequence(uploadFutures).map(_ => ())
  }

  def main(args: Array[String]): Unit = {
    FirebaseUtils.initializeFirebase()
    println("Uploading song metadata...")

    // Wait for all uploads to complete
    val uploadResult = uploadSongsMetadata()
    Await.result(uploadResult, 30.seconds) // Adjust timeout as needed

    println("All metadata uploaded. Exiting.")
  }
}
