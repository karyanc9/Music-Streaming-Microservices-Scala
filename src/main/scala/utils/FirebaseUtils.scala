package utils

import com.google.firebase.{FirebaseApp, FirebaseOptions}
import com.google.firebase.auth.{FirebaseAuth, FirebaseAuthException, UserRecord}
import com.google.firebase.database.{DataSnapshot, DatabaseError, DatabaseReference, FirebaseDatabase, Query, ValueEventListener}
import com.google.firebase.database.GenericTypeIndicator


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

  /** Initialize Firebase App */
  def initializeFirebase(): Unit = {
    if (!firebaseInitialized) {
      Try {
        val serviceAccount = new FileInputStream("src/main/firebase/firebase-config.json")
        val options = FirebaseOptions.builder()
          .setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(serviceAccount))
          .setDatabaseUrl("https://spotify-8b642-default-rtdb.asia-southeast1.firebasedatabase.app")  // Use your actual Firebase database URL
          .build()

        FirebaseApp.initializeApp(options)
        firebaseInitialized = true
        println("Firebase initialized successfully.")
      } match {
        case Success(_) => // Initialization successful
        case Failure(exception) =>
          println(s"Failed to initialize Firebase: ${exception.getMessage}")
          throw exception
      }
    }
  }

  /** Save song metadata to Firebase Realtime Database */
  def saveSongMetadata2(songId: String, metadata: Map[String, Any]): Future[Boolean] = {
    initializeFirebase()
    val promise = Promise[Boolean]()
    val songRef = database.child("songs").child(songId)

    val futureResult = songRef.setValueAsync(metadata)
    futureResult.addListener(new Runnable {
      override def run(): Unit = {
        try {
          futureResult.get()
          println(s"Song metadata saved successfully for songId: $songId")
          promise.success(true)
        } catch {
          case e: Exception =>
            println(s"Failed to save song metadata: ${e.getMessage}")
            promise.failure(e)
        }
      }
    }, scala.concurrent.ExecutionContext.global)

    promise.future
  }

  def saveSongMetadata(songId: String, metadata: Map[String, Any]): Future[Boolean] ={
    initializeFirebase()
    val promise = Promise[Boolean]()
    val songRef = database.child("songs").child(songId)

    // Convert Scala Map to Java HashMap
    val javaMetadata = new java.util.HashMap[String, Any]()
    metadata.foreach { case (key, value) =>
      javaMetadata.put(key, value)
    }

    val futureResult = songRef.setValueAsync(javaMetadata)
    futureResult.addListener(new Runnable {
      override def run(): Unit = {
        try {
          futureResult.get()
          println(s"Song metadata saved successfully for songId: $songId")
          promise.success(true)
        } catch {
          case e: Exception =>
            println(s"Failed to save song metadata: ${e.getMessage}")
            promise.failure(e)
        }
      }
    }, scala.concurrent.ExecutionContext.global)

    promise.future
  }

  /** Register a new user with Firebase Authentication */
  def registerUser(username: String, password: String): Future[Option[String]] = {
    initializeFirebase()
    val promise = Promise[Option[String]]()

    Try {
      val userRecord = firebaseAuth.createUser(
        new UserRecord.CreateRequest()
          .setEmail(username)
          .setPassword(password)
      )
      promise.success(Some(s"User ${userRecord.getEmail} registered successfully."))
    } match {
      case Success(_) => // Success handled above
      case Failure(exception) =>
        println(s"Failed to register user: ${exception.getMessage}")
        promise.success(None)
    }

    promise.future
  }

  /** Login a user with Firebase Authentication */
  def loginUser(username: String, password: String): Future[Option[String]] = {
    initializeFirebase()
    val promise = Promise[Option[String]]()

    // Check if user exists
    Try {
      val userRecord = firebaseAuth.getUserByEmail(username)
      if (userRecord != null) {
        // Only create session once, and directly pass the result to the promise
        createSession(username).onComplete {
          case Success(token) =>
            // Only send a successful response after session creation
            println(s"Login successful! Session started with token: $token")
            promise.success(Some(s"Login successful! Session token: $token"))
          case Failure(e) =>
            println(s"Login successful, but session creation failed: ${e.getMessage}")
            promise.success(Some("Login successful, but session creation failed."))
        }
      } else {
        println("User not found")
        promise.success(None)  // User does not exist
      }
    } match {
      case Success(_) => // Success is handled in the above block
      case Failure(exception: FirebaseAuthException) =>
        println(s"Login failed: ${exception.getMessage}")
        promise.success(None)  // Return None on FirebaseAuthException
      case Failure(exception) =>
        println(s"Unexpected error: ${exception.getMessage}")
        promise.success(None)  // Handle other errors
    }

    promise.future
  }

  /** Create a session for a user */
  def createSession(username: String): Future[String] = {
    initializeFirebase()
    val token = UUID.randomUUID().toString // Generate a unique session token
    val promise = Promise[String]()
    val sessionRef = database.child("sessions").child(token)

    // Properly sanitize the map keys
    val sessionData = Map(
      "username" -> username,
      "createdAt" -> System.currentTimeMillis()
    )

    val futureResult = sessionRef.setValueAsync(sessionData.asJava) // Convert Scala Map to Java Map
    futureResult.addListener(new Runnable {
      override def run(): Unit = {
        try {
          futureResult.get()
          println(s"Session created for $username with token $token")
          promise.success(token)
        } catch {
          case e: Exception =>
            println(s"Failed to create session for $username: ${e.getMessage}")
            promise.failure(e)
        }
      }
    }, scala.concurrent.ExecutionContext.global)

    promise.future
  }

  /** Check if an existing session already exists for a user */
  def checkExistingSession(username: String): Future[Option[String]] = {
    initializeFirebase()
    val promise = Promise[Option[String]]()

    val sessionRef = database.child("sessions")
    sessionRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        if (snapshot.exists()) {
          // If a session is found for the username, return the session token
          val sessionToken = snapshot.getChildren.iterator().next().getKey
          promise.success(Some(sessionToken))
        } else {
          promise.success(None) // No existing session
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        println(s"Error checking for existing session: ${error.getMessage}")
        promise.failure(new Exception(error.getMessage))
      }
    })

    promise.future
  }

  /** Validate a session token */
  def validateSession(token: String): Future[Boolean] = {
    initializeFirebase()
    val promise = Promise[Boolean]()
    val sessionRef = database.child("sessions").child(token)

    sessionRef.addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        promise.success(snapshot.exists()) // Return true if token exists
      }

      override def onCancelled(error: DatabaseError): Unit = {
        println(s"Error validating session: ${error.getMessage}")
        promise.failure(new Exception(error.getMessage))
      }
    })

    promise.future
  }

  /** Retrieve a playlist from Firebase Realtime Database */
  def getPlaylist(playlistId: String): Future[Option[Map[String, Any]]] = {
    initializeFirebase()
    // Placeholder implementation
    Future.successful(Some(Map("id" -> playlistId, "name" -> "Sample Playlist")))
  }

  /** Search for a song by title in Firebase Realtime Database */
  def searchSong2(title: String): Future[List[Map[String, Any]]] = {
    initializeFirebase()
    val promise = Promise[List[Map[String, Any]]]()
    val songsRef = database.child("songs")
    val query: Query = songsRef.orderByChild("title").equalTo(title)

    query.addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        if (snapshot.exists()) {
          val result = snapshot.getChildren.asScala.toList.map { child =>
            child.getValue(classOf[java.util.Map[String, Any]]).asInstanceOf[Map[String, Any]]
          }
          promise.success(result)
        } else {
          promise.success(Nil) // Return an empty list if no songs are found
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        println(s"Error searching for song: ${error.getMessage}")
        promise.failure(new Exception(error.getMessage))
      }
    })

    promise.future
  }

  def searchSong(title: String): Future[List[Map[String, Any]]] = {
    initializeFirebase()
    val promise = Promise[List[Map[String, Any]]]()
    val songsRef = database.child("songs")
    val query = database.child("songs").orderByChild("title").equalTo(title)
    //val query: Query = songsRef.orderByChild("title").equalTo(title)

    println(s"Searching for songs with title: $title")

    query.addListenerForSingleValueEvent(new ValueEventListener {
//      override def onDataChange(snapshot: DataSnapshot): Unit = {
//        println(s"Snapshot exists: ${snapshot.exists()}")
//        if (snapshot.exists()) {
////          println(s"Snapshot exists: ${snapshot.exists()} - Children count: ${snapshot.getChildrenCount}")
////          snapshot.getChildren.asScala.toList.foreach { child =>
////            println(s"Child data: ${child.getValue}")
////          }
//          val result = snapshot.getChildren.asScala.toList.map { child =>
//            val songData = child.getValue(classOf[java.util.Map[String, Any]]).asInstanceOf[Map[String, Any]]
//            println(s"Parsed song: $songData")
//            songData
//          }
//          println(s"searchSong completed with: $result")
//          println("Invoking promise.success with result")
//          promise.success(result)
//        } else {
//          println("No matching songs found in Firebase.")
//          promise.success(Nil) // Return an empty list if no songs are found
//        }
//      }

      override def onDataChange(snapshot: DataSnapshot): Unit = {
        //println(s"Snapshot exists: ${snapshot.exists()}")
        if (snapshot.exists()) {
          //println(s"Raw snapshot data: ${snapshot.getValue}")
          //println(s"Children count: ${snapshot.getChildrenCount}")

          val result = snapshot.getChildren.asScala.toList.flatMap { child =>
            //println(s"Processing child: ${child.getKey}")
            val rawValue = child.getValue
            //println(s"Raw child data: $rawValue, Type: ${rawValue.getClass}")

            try {
              // Use GenericTypeIndicator to handle generic types
              val typeIndicator = new GenericTypeIndicator[java.util.Map[String, Any]]() {}
              val songNode = child.getValue(typeIndicator)
              if (songNode == null) {
                println(s"Child has null data: ${child.getKey}")
                None
              } else {
                val scalaData = songNode.asScala.toMap
                //println(s"Parsed song: $scalaData")
                Some(scalaData)
              }
            } catch {
              case e: Exception =>
                println(s"Error parsing child data: ${e.getMessage}")
                None
            }
          }

          if (result.isEmpty) {
            println("No valid songs found, completing with empty list")
            promise.success(Nil)
          } else {
            //println(s"Valid songs found: $result, completing promise")
            promise.success(result)
          }
        } else {
          println("No matching songs found in Firebase.")
          promise.success(Nil)
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        println(s"Error searching for song: ${error.getMessage}")
        promise.failure(new Exception(error.getMessage))
      }
    })

    promise.future
  }

}








