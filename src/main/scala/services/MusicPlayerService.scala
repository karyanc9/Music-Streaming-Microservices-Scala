package services

import scalafx.scene.control.Label
import scalafx.scene.image.{Image, ImageView}
import javafx.scene.media.{Media, MediaPlayer}
import javafx.embed.swing.JFXPanel
import java.io.File

class MusicPlayerService(songDir: String) {
  private val songs = new File(songDir).listFiles.filter(_.getName.endsWith(".mp3")).toList
  private val albumCovers = Map(
    "song1.mp3" -> "song1.jpg",
    "song2.mp3" -> "song2.jpg",
    "song3.mp3" -> "song3.jpg"
  )
  private var currentIndex = 0
  private var mediaPlayer: Option[MediaPlayer] = None

  // Preload JavaFX Toolkit
  new JFXPanel()

  def play(songTitleLabel: Label, albumCoverImageView: ImageView): Unit = {
    if (songs.isEmpty) {
      songTitleLabel.text = "No Songs Available"
      return
    }

    // If no media player or song changed, load current song
    if (mediaPlayer.isEmpty || mediaPlayer.get.getStatus != MediaPlayer.Status.PLAYING) {
      loadSong(currentIndex, albumCoverImageView)
    }

    mediaPlayer.foreach(_.play())
    songTitleLabel.text = s"Playing: ${songs(currentIndex).getName}"
  }

  def pause(): Unit = {
    mediaPlayer.foreach(_.pause())
  }

  def next(songTitleLabel: Label, albumCoverImageView: ImageView): Unit = {
    if (songs.nonEmpty) {
      currentIndex = (currentIndex + 1) % songs.size
      loadSong(currentIndex, albumCoverImageView)
      play(songTitleLabel, albumCoverImageView)
    }
  }

  def previous(songTitleLabel: Label, albumCoverImageView: ImageView): Unit = {
    if (songs.nonEmpty) {
      currentIndex = (currentIndex - 1 + songs.size) % songs.size
      loadSong(currentIndex, albumCoverImageView)
      play(songTitleLabel, albumCoverImageView)
    }
  }

  private def loadSong(index: Int, albumCoverImageView: ImageView): Unit = {
    mediaPlayer.foreach(_.dispose())
    val media = new Media(songs(index).toURI.toString)
    mediaPlayer = Some(new MediaPlayer(media))

    // Update the album cover
    val songFileName = songs(index).getName
    val albumCoverFile = new File(s"songs/${albumCovers.getOrElse(songFileName, "default.jpg")}")
    val albumCoverImage = new Image(albumCoverFile.toURI.toString)
    albumCoverImageView.image = albumCoverImage
  }
}
