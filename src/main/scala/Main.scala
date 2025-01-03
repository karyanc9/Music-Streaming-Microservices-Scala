package main

import UI.SpotifyLoginUI
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import actors.{MusicPlayerActor, PlaylistServiceActor, SongLibraryActor, SystemIntegratorActor, UserServiceActor}
import actors.UserServiceActor.{DeleteUser, LoginUser, RegisterUser, UpdatePassword, ValidateSession}
import utils.FirebaseUtils
import akka.actor.typed.ActorRef
import protocols.SongProtocols.{AddSong, SearchSong}

object Main extends App {
  // Initialize Firebase
  FirebaseUtils.initializeFirebase()

  // Initialize UserServiceActor - Create the main ActorSystem once
  val userService: ActorRef[UserServiceActor.Command] = ActorSystem(UserServiceActor(), "UserServiceActor")
  val songLibrary: ActorRef[protocols.SongProtocols.Command] = ActorSystem(SongLibraryActor(), "SongLibraryActor")
  val musicPlayerActor: ActorSystem[protocols.SongProtocols.Command] = ActorSystem(MusicPlayerActor(), "MusicPlayerActor")
  val playlistServiceActor: ActorRef[protocols.PlaylistProtocols.Command] = ActorSystem(PlaylistServiceActor(), "PlaylistServiceActor")

  // Initialize SystemIntegratorActor, passing all required actors
  implicit val systemIntegrator: ActorRef[SystemIntegratorActor.Command] = ActorSystem(
    SystemIntegratorActor(userService, songLibrary, playlistServiceActor, musicPlayerActor),
    "SystemIntegratorActor"
  )

  // Launch App
  SpotifyLoginUI.main(Array())


}
