package utils

import com.google.firebase.{FirebaseApp, FirebaseOptions}
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.{FirebaseAuth, UserRecord}
import com.google.firebase.database.{DatabaseReference, FirebaseDatabase}
import java.io.FileInputStream

object FirebaseUtils {
  // Initialize Firebase App
  def initializeFirebase(): Unit = {
    val serviceAccountPath = "src/main/firebase/firebase-config.json"
    val serviceAccount = new FileInputStream(serviceAccountPath)

    val options = FirebaseOptions.builder()
      .setCredentials(GoogleCredentials.fromStream(serviceAccount))
      .setDatabaseUrl("https://spotify-8b642-default-rtdb.asia-southeast1.firebasedatabase.app")
      .build()

    if (FirebaseApp.getApps.isEmpty) {
      FirebaseApp.initializeApp(options)
      println("Firebase successfully initialized.")
    } else {
      println("Firebase is already initialized.")
    }
  }

  // Register a user using Firebase Admin SDK
  def registerUser(email: String, password: String): Option[String] = {
    try {
      val request = new UserRecord.CreateRequest()
        .setEmail(email)
        .setPassword(password)

      val userRecord = FirebaseAuth.getInstance().createUser(request)
      Some(s"User ${userRecord.getEmail} registered successfully.")
    } catch {
      case ex: Exception =>
        println(s"Failed to register user: ${ex.getMessage}")
        None
    }
  }

  // Login simulation (Firebase Admin SDK doesn't support user login)
  def loginUser(email: String, password: String): Option[String] = {
    println(s"Simulated login for user $email (Password verification not supported in Admin SDK)")
    Some(s"User $email logged in successfully.")
  }

  // Save a playlist to Firebase Realtime Database
  def savePlaylist(playlistId: String, data: Map[String, Any]): Unit = {
    val ref = getDatabaseRef(s"playlists/$playlistId")
    ref.setValueAsync(data)
  }

  // Retrieve a playlist from Firebase Realtime Database
  def getPlaylist(playlistId: String): Option[Map[String, Any]] = {
    println(s"Simulated retrieval of playlist: $playlistId")
    None
  }

  // Save song metadata to Firebase Realtime Database
  def saveSongMetadata(songId: String, metadata: Map[String, String]): Unit = {
    val ref = getDatabaseRef(s"songs/$songId")
    ref.setValueAsync(metadata)
  }

  // Search for a song in Firebase Realtime Database
  def searchSong(title: String): Option[String] = {
    println(s"Simulated search for song: $title")
    None
  }

  // Get a Firebase Database reference
  private def getDatabaseRef(path: String): DatabaseReference = {
    FirebaseDatabase.getInstance().getReference(path)
  }
}
