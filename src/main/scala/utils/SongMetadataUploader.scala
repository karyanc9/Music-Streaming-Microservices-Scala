package utils

import utils.FirebaseUtils
import scala.util.{Success, Failure}
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

// used to upload songs to the realtime library for use

object SongMetadataUploader {
  def uploadSongsMetadata(): Future[Unit] = {

    // Map of already uploaded songs , to add a new song to the library just create the map and run the program
    val songs = Map(
//      "song1" -> Map(
//        "id" -> "song1",
//        "title" -> "Mamma Mia",
//        "artist" -> "ABBA",
//        "genre" -> "Pop",
//        "duration" -> "3:30",
//        "filePath" -> "songs/song1.mp3",
//        "imagePath" -> "songs/song1.jpg"
//      ),
//      "song2" -> Map(
//        "id" -> "song2",
//        "title" -> "Billie Jean",
//        "artist" -> "Michael Jackson",
//        "genre" -> "Disco",
//        "duration" -> "4:45",
//        "filePath" -> "songs/song2.mp3",
//        "imagePath" -> "songs/song2.jpg"
//      ),
//      "song3" -> Map(
//        "id" -> "song3",
//        "title" -> "Bohemian Rhapsody",
//        "artist" -> "Queen",
//        "genre" -> "Hard rock",
//        "duration" -> "5:59",
//        "filePath" -> "songs/song3.mp3",
//        "imagePath" -> "songs/song3.jpg"
//      ),
//    "song4" -> Map(
//      "id" -> "song4",
//      "title" -> "Can't Help Falling In Love",
//      "artist" -> "Elvis Presley",
//      "genre" -> "Rock & Roll Ballad",
//      "duration" -> "3:00",
//      "filePath" -> "songs/song4.mp3",
//      "imagePath" -> "songs/song4.jpg"
//    )
//      "song5" -> Map(
//        "id" -> "song5",
//        "title" -> "Iris",
//        "artist" -> "Goo Goo Dolls",
//        "genre" -> "Rock Ballad",
//        "duration" -> "4:49",
//        "filePath" -> "songs/song5.mp3",
//        "imagePath" -> "songs/song5.jpg"
//      ),
//      "song6" -> Map(
//        "id" -> "song6",
//        "title" -> "Leave A Light On",
//        "artist" -> "Belinda Carlisle",
//        "genre" -> "Pop Rock",
//        "duration" -> "4:14",
//        "filePath" -> "songs/song6.mp3",
//        "imagePath" -> "songs/song6.jpg"
//      )

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
