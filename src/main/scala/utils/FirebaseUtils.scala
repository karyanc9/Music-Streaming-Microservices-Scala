package utils

import com.google.firebase.{FirebaseApp, FirebaseOptions}
import com.google.firebase.auth.{FirebaseAuth, FirebaseAuthException, UserRecord}
import com.google.firebase.database.{DataSnapshot, DatabaseError, DatabaseReference, FirebaseDatabase, ValueEventListener}
import java.io.FileInputStream
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}
import java.util.UUID

object FirebaseUtils {
  private var firebaseInitialized = false
  private lazy val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
  private lazy val database: DatabaseReference = FirebaseDatabase.getInstance().getReference

  // Initialize Firebase
  def initializeFirebase(): Unit = {
    if (!firebaseInitialized) {
      Try {
        val serviceAccount = new FileInputStream("src/main/firebase/firebase-config.json")
        val options = FirebaseOptions.builder()
          .setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(serviceAccount))
          .setDatabaseUrl("https://spotify-8b642-default-rtdb.asia-southeast1.firebasedatabase.app")
          .build()

        FirebaseApp.initializeApp(options)
        firebaseInitialized = true
        println("Firebase initialized successfully.")
      } match {
        case Success(_) =>
        case Failure(exception) =>
          println(s"Failed to initialize Firebase: ${exception.getMessage}")
          throw exception
      }
    }
  }

  // Create a session for a user
  def createSession(username: String): Future[String] = {
    val promise = Promise[String]()
    val sessionToken = UUID.randomUUID().toString // Generate a unique session token
    val sessionRef = database.child("sessions").child(sessionToken)

    val sessionData = Map(
      "username" -> username,
      "createdAt" -> System.currentTimeMillis()
    )

    sessionRef.setValueAsync(sessionData.asJava).addListener(new Runnable {
      override def run(): Unit = {
        try {
          println(s"Session created for $username with token: $sessionToken")
          promise.success(sessionToken)
        } catch {
          case e: Exception =>
            println(s"Failed to create session for $username: ${e.getMessage}")
            promise.failure(e)
        }
      }
    }, scala.concurrent.ExecutionContext.global)

    promise.future
  }

  // Check if an existing session already exists for a user
  def checkExistingSession(username: String): Future[Option[String]] = {
    val promise = Promise[Option[String]]()
    val sessionRef = database.child("sessions")

    sessionRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        if (snapshot.exists()) {
          val sessionToken = snapshot.getChildren.iterator().next().getKey
          promise.success(Some(sessionToken))
        } else {
          promise.success(None) // No existing session
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        promise.failure(new Exception(s"Firebase session check failed: ${error.getMessage}"))
      }
    })

    promise.future
  }

  // Validate a session token
  def validateSession(token: String): Future[Boolean] = {
    val promise = Promise[Boolean]()
    val sessionRef = database.child("sessions").child(token)

    sessionRef.addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        promise.success(snapshot.exists()) // Return true if the token exists
      }

      override def onCancelled(error: DatabaseError): Unit = {
        promise.failure(new Exception(s"Firebase session validation failed: ${error.getMessage}"))
      }
    })

    promise.future
  }

  // Fetch all songs from Firebase
  def fetchAllSongs(): Future[List[Map[String, Any]]] = {
    val promise = Promise[List[Map[String, Any]]]()
    val songsRef = database.child("songs")

    songsRef.addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        if (snapshot.exists()) {
          val songs = snapshot.getChildren.asScala.toList.map { child =>
            child.getValue(classOf[java.util.Map[String, Any]]).asScala.toMap
          }
          promise.success(songs)
        } else {
          promise.success(Nil) // No songs found
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        promise.failure(new Exception(s"Firebase fetch failed: ${error.getMessage}"))
      }
    })

    promise.future
  }

  // Fetch playlist by playlistId from Firebase
  def getPlaylist(playlistId: String): Future[Option[Map[String, Any]]] = {
    val promise = Promise[Option[Map[String, Any]]]()
    val playlistRef = database.child("playlists").child(playlistId)

    playlistRef.addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        if (snapshot.exists()) {
          val playlistData = snapshot.getValue(classOf[java.util.Map[String, Any]]).asScala.toMap
          promise.success(Some(playlistData))
        } else {
          promise.success(None) // Playlist not found
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        promise.failure(new Exception(s"Firebase fetch failed: ${error.getMessage}"))
      }
    })

    promise.future
  }

  // Fetch all playlists from Firebase
  def fetchAllPlaylists(): Future[List[Map[String, Any]]] = {
    val promise = Promise[List[Map[String, Any]]]()
    val playlistsRef = database.child("playlists")

    playlistsRef.addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        if (snapshot.exists()) {
          val playlists = snapshot.getChildren.asScala.toList.map { child =>
            child.getValue(classOf[java.util.Map[String, Any]]).asScala.toMap
          }
          promise.success(playlists)
        } else {
          promise.success(Nil) // No playlists found
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        promise.failure(new Exception(s"Firebase fetch failed: ${error.getMessage}"))
      }
    })

    promise.future
  }

  // Save song metadata to Firebase
  def saveSongMetadata(songId: String, metadata: Map[String, Any]): Future[Boolean] = {
    val promise = Promise[Boolean]()
    val songRef = database.child("songs").child(songId)

    songRef.setValueAsync(metadata.asJava).addListener(new Runnable {
      override def run(): Unit = {
        try {
          println(s"Successfully saved metadata for songId: $songId")
          promise.success(true)
        } catch {
          case ex: Exception =>
            println(s"Failed to save song metadata for songId: $songId - Error: ${ex.getMessage}")
            promise.failure(ex)
        }
      }
    }, scala.concurrent.ExecutionContext.global)

    promise.future
  }

  // Search for a song by title in Firebase
  def searchSong(title: String): Future[List[Map[String, Any]]] = {
    val promise = Promise[List[Map[String, Any]]]()
    val songsRef = database.child("songs")
    val query = songsRef.orderByChild("title").equalTo(title)

    query.addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        if (snapshot.exists()) {
          val result = snapshot.getChildren.asScala.toList.map { child =>
            child.getValue(classOf[java.util.Map[String, Any]]).asScala.toMap
          }
          promise.success(result)
        } else {
          promise.success(Nil) // No matching songs found
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        promise.failure(new Exception(s"Firebase search failed: ${error.getMessage}"))
      }
    })

    promise.future
  }

  // Register a new user with Firebase Authentication
  def registerUser(username: String, password: String): Future[Option[String]] = {
    val promise = Promise[Option[String]]()
    Try {
      val userRecord = firebaseAuth.createUser(
        new UserRecord.CreateRequest()
          .setEmail(username)
          .setPassword(password)
      )
      promise.success(Some(s"User ${userRecord.getEmail} registered successfully."))
    } match {
      case Success(_) => // Success is handled in the future
      case Failure(exception) =>
        println(s"Failed to register user: ${exception.getMessage}")
        promise.success(None)
    }
    promise.future
  }

  // Login a user with Firebase Authentication
  def loginUser(username: String, password: String): Future[Option[String]] = {
    val promise = Promise[Option[String]]()
    Try {
      val userRecord = firebaseAuth.getUserByEmail(username)
      if (userRecord != null) {
        promise.success(Some(s"User ${userRecord.getEmail} logged in successfully."))
      } else {
        promise.success(None) // User not found
      }
    } match {
      case Success(_) => // Success is handled above
      case Failure(exception: FirebaseAuthException) =>
        println(s"Failed to login user: ${exception.getMessage}")
        promise.success(None)
    }
    promise.future
  }
}
