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

    // Normalize the title to lowercase for case-insensitive search
    val normalizedTitle = title.toLowerCase

    println(s"Searching for songs with title: $title (case-insensitive)")

    // Firebase query for all songs
    val songsRef = database.child("songs")

    songsRef.addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        if (snapshot.exists()) {
          println(s"Snapshot exists: ${snapshot.exists()}")

          // Iterate through each child node and filter by case-insensitive title match
          val result = snapshot.getChildren.asScala.toList.flatMap { child =>
            val rawValue = child.getValue
            try {
              val typeIndicator = new GenericTypeIndicator[java.util.Map[String, Any]]() {}
              val songNode = child.getValue(typeIndicator)
              if (songNode == null) {
                println(s"Child has null data: ${child.getKey}")
                None
              } else {
                val scalaData = songNode.asScala.toMap
                // Case-insensitive title search
                scalaData.get("title") match {
                  case Some(titleData: String) if titleData.toLowerCase.contains(normalizedTitle) =>
                    Some(scalaData) // Title matches, include the song
                  case _ =>
                    None // Title doesn't match, exclude the song
                }
              }
            } catch {
              case e: Exception =>
                println(s"Error parsing child data: ${e.getMessage}")
                None
            }
          }

          if (result.isEmpty) {
            println("No matching songs found.")
            promise.success(Nil) // Complete with empty list if no matches
          } else {
            println(s"Found ${result.size} matching song(s).")
            promise.success(result) // Complete with filtered results
          }
        } else {
          println("No matching songs found in Firebase.")
          promise.success(Nil) // Complete with empty list if no songs exist
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        println(s"Error searching for song: ${error.getMessage}")
        promise.failure(new Exception(error.getMessage)) // Complete with failure if cancelled
      }
    })

    // Return the future from the Promise
    promise.future
  }


  def fetchAllSongs(): Future[List[Map[String, Any]]] = {
    initializeFirebase()
    val promise = Promise[List[Map[String, Any]]]()
    val songsRef = database.child("songs")

    println("Fetching songs from Firebase...")

    songsRef.addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        println(s"onDataChange triggered. Snapshot exists: ${snapshot.exists()}")

        if (snapshot.exists()) {
          println(s"Number of children: ${snapshot.getChildrenCount}")

          val typeIndicator = new GenericTypeIndicator[java.util.Map[String, Any]]() {}
          val songs = snapshot.getChildren.asScala.toList.collect {
            case child =>
              println(s"Processing child: ${child.getKey}")
              try {
                val songData = child.getValue(typeIndicator).asScala.toMap
                println(s"Fetched child data: $songData")
                songData
              } catch {
                case e: Exception =>
                  println(s"Error processing child ${child.getKey}: ${e.getMessage}")
                  null // Return null for problematic entries
              }
          }.filter(_ != null) // Remove null entries

          println(s"Fetched songs: $songs")
          promise.success(songs)
        } else {
          println("No songs found in Firebase.")
          promise.success(Nil) // No songs available
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        println(s"Error fetching songs: ${error.getMessage}")
        promise.failure(new Exception(error.getMessage))
      }
    })

    promise.future
  }

  /** Fetch all song metadata from Firebase Realtime Database  */
  def fetchAllSongs2(): Future[List[Map[String, Any]]] = {
    initializeFirebase()
    val promise = Promise[List[Map[String, Any]]]()
    val songsRef = database.child("songs")

    println("Fetching songs from Firebase...")

    songsRef.addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        println(s"onDataChange triggered. Snapshot exists: ${snapshot.exists()}")
        if (snapshot.exists()) {
          val songs = snapshot.getChildren.asScala.toList.map { child =>
            println(s"Processing child: ${child.getKey}")
            try {
              val songData = child.getValue(classOf[java.util.Map[String, Any]]).asScala.toMap
              println(s"Fetched child data: $songData")
              songData
              //Some(songData) // Add valid song data to the list
            } catch {
              case e: Exception =>
                println(s"Error processing child ${child.getKey}: ${e.getMessage}")
                null // Skip problematic entries
            }
            //child.getValue(classOf[java.util.Map[String, Any]]).asScala.toMap
//            val songData = child.getValue(classOf[java.util.Map[String, Any]]).asScala.toMap
//            println(s"Song data: $songData")
//            songData
          }.filter(_ != null)
          println(s"Fetched songs: $songs")
          promise.success(songs)
        } else {
          println("No songs found in Firebase.")
          promise.success(Nil) // No songs found
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        println(s"Error fetching songs: ${error.getMessage}")
        promise.failure(new Exception(error.getMessage))
      }
    })

    promise.future
  }


  // Creating a playlist
  def createPlaylist(name: String): Future[String] = {
    val playlistId = UUID.randomUUID().toString // Generate a unique ID for the playlist
    val playlistRef = database.child("playlists").child(playlistId)

    val playlistData = Map(
      "id" -> playlistId,
      "name" -> name
    )

    val promise = Promise[String]()

    playlistRef.setValueAsync(playlistData.asJava).addListener(new Runnable {
      override def run(): Unit = {
        try {
          promise.success(playlistId) // On success, complete the promise with the playlist ID
        } catch {
          case e: Exception =>
            promise.failure(new Exception(s"Failed to create playlist: ${e.getMessage}")) // On failure, fail the promise
        }
      }
    }, scala.concurrent.ExecutionContext.global)

    promise.future
  }



  def addSongToPlaylist(playlistId: String, songTitle: String): Future[Unit] = {
    val playlistRef = database.child("playlists").child(playlistId).child("songs")
    val promise = Promise[Unit]()

    // Fetch the song details by title
    fetchSongDetails(songTitle).onComplete {
      case Success(Some((songId, songData))) =>
        // Use the songId as the key in the playlist
        val songRef = playlistRef.child(songId) // Use songId as key
        songRef.setValueAsync(songData.asJava).addListener(new Runnable {
          override def run(): Unit = {
            promise.success(()) // Successfully added the song
          }
        }, scala.concurrent.ExecutionContext.global)

      case Success(None) =>
        // Song with the given title not found
        promise.failure(new Exception(s"Song with title '$songTitle' not found."))

      case Failure(exception) =>
        // Failed to fetch song details
        promise.failure(new Exception(s"Failed to fetch song details: ${exception.getMessage}"))
    }

    promise.future
  }

  def fetchSongDetails(songTitle: String): Future[Option[(String, Map[String, Any])]] = {
    val songsRef = database.child("songs")
    val promise = Promise[Option[(String, Map[String, Any])]]()

    // Convert the search title to lowercase for case-insensitive matching
    val searchTitle = songTitle.toLowerCase.trim

    // Query to get all songs, then filter them locally by the lowercase title
    songsRef.addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        if (snapshot.exists()) {
          // Check each song for a case-insensitive match by comparing lowercase versions
          val songDataOpt = snapshot.getChildren.asScala.collectFirst {
            case child if Option(child.child("title").getValue).exists(_.toString.toLowerCase == searchTitle) =>
              val songData = child.getValue(new GenericTypeIndicator[java.util.Map[String, Any]]() {}).asScala.toMap
              val songId = child.getKey // Use Firebase's child key (songId)
              (songId, songData) // Return songId and the song data
          }

          promise.success(songDataOpt)
        } else {
          promise.success(None) // Song with the given title not found
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        promise.failure(new Exception(s"Error fetching song details for title '$songTitle': ${error.getMessage}"))
      }
    })

    promise.future
  }


  def getPlaylistSongs(playlistId: String): Future[List[Map[String, Any]]] = {
    val playlistRef = database.child("playlists").child(playlistId).child("songs")
    val promise = Promise[List[Map[String, Any]]]()

    println(s"Attempting to fetch songs for playlist ID: $playlistId")

    playlistRef.addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        println(s"onDataChange triggered for playlist ID: $playlistId")

        if (snapshot.exists()) {
          println(s"Snapshot exists for playlist ID: $playlistId. Data: ${snapshot.getValue}")

          // Fetch each song as a nested object under `songs`
          val rawData = snapshot.getValue.asInstanceOf[java.util.Map[String, java.util.Map[String, Object]]]

          // If data is a map of songId -> songData
          val songs = rawData.asScala.map { case (songId, songData) =>
            // Convert the nested song data to a Map
            songData.asScala.toMap + ("songId" -> songId) // Add the songId to the song data map
          }.toList

          if (songs.nonEmpty) {
            println(s"Fetched ${songs.size} song(s) for playlist ID: $playlistId")
            songs.foreach(song => println(s"Song data: $song"))
            promise.success(songs)
          } else {
            println(s"No songs found in playlist ID: $playlistId")
            promise.success(Nil) // No songs in the playlist
          }
        } else {
          println(s"Snapshot does not exist for playlist ID: $playlistId")
          promise.success(Nil) // No songs in the playlist
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        println(s"Error fetching playlist songs for ID: $playlistId. Error: ${error.getMessage}")
        promise.failure(new Exception(s"Error fetching playlist songs: ${error.getMessage}"))
      }
    })

    promise.future
  }





  // removing a song from the playlist

  def removeSongFromPlaylist(playlistId: String, songId: String): Future[Unit] = {
    val playlistRef = database.child("playlists").child(playlistId).child("songs")
    val promise = Promise[Unit]()

    playlistRef.orderByChild("id").equalTo(songId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        if (snapshot.exists()) {
          snapshot.getChildren.forEach(child => {
            child.getRef.removeValueAsync().addListener(new Runnable {
              override def run(): Unit = {
                println(s"Song with ID $songId deleted successfully.")
                promise.success(()) // Successfully deleted the song
              }
            }, scala.concurrent.ExecutionContext.global)
          })
        } else {
          println(s"Song with ID $songId not found.")
          promise.success(()) // No change, resolve promise
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        println(s"Database error: ${error.getMessage}")
        promise.failure(new Exception(s"Error removing song from playlist: ${error.getMessage}"))
      }
    })

    promise.future
  }



  // getting a playlist

  def getPlaylists(playlistId: String): Future[Option[Map[String, Any]]] = {
    val playlistRef = database.child("playlists").child(playlistId)
    val promise = Promise[Option[Map[String, Any]]]()

    playlistRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        if (snapshot.exists()) {
          val playlistData = snapshot.getValue(new GenericTypeIndicator[java.util.Map[String, Any]]() {}).asScala.toMap
          promise.success(Some(playlistData))
        } else {
          promise.success(None) // No playlist found
        }
      }

      override def onCancelled(error: DatabaseError): Unit = {
        promise.failure(new Exception(s"Error fetching playlist: ${error.getMessage}"))
      }
    })

    promise.future
  }


  // getting all playlists

  def getAllPlaylists: Future[List[Map[String, Any]]] = {
    val playlistsRef = database.child("playlists")
    val promise = Promise[List[Map[String, Any]]]()

    playlistsRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        val playlists = snapshot.getChildren.asScala.toList.map { child =>
          child.getValue(new GenericTypeIndicator[java.util.Map[String, Any]]() {}).asScala.toMap
        }
        promise.success(playlists)
      }

      override def onCancelled(error: DatabaseError): Unit = {
        promise.failure(new Exception(s"Error fetching playlists: ${error.getMessage}"))
      }
    })

    promise.future
  }

  // removing a playlist
  def removePlaylist(playlistId: String): Future[Unit] = {
    val playlistRef = database.child("playlists").child(playlistId)
    val promise = Promise[Unit]()

    playlistRef.removeValueAsync().addListener(new Runnable {
      override def run(): Unit = {
        try {
          promise.success(()) // Successfully removed the playlist
        } catch {
          case e: Exception =>
            promise.failure(new Exception(s"Failed to remove playlist: ${e.getMessage}"))
        }
      }
    }, scala.concurrent.ExecutionContext.global)

    promise.future
  }

  def listenForPlaylistChanges(onUpdate: => Unit): Unit = {
    val playlistRef = FirebaseDatabase.getInstance().getReference("playlists")

    playlistRef.addValueEventListener(new ValueEventListener {
      override def onDataChange(snapshot: DataSnapshot): Unit = {
        // This will be triggered when data changes in the "playlists" node
        println("Playlists data changed, updating UI...")
        onUpdate // Call the provided onUpdate function to refresh UI
      }

      override def onCancelled(error: DatabaseError): Unit = {
        println(s"Failed to listen for playlist changes: ${error.getMessage}")
      }
    })
  }




}








