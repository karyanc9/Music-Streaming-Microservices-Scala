package protocols

object PlaylistProtocols {

  sealed trait Command

  case class CreatePlaylist(name: String) extends Command
  case class AddSongToPlaylist(playlistId: String, songId: String) extends Command
  case class GetPlaylist(playlistId: String) extends Command
  case class GetPlaylistSongs(playlistId: String) extends Command
  case class RemoveSongFromPlaylist(playlistId: String, songId: String) extends Command
  case class RemovePlaylist(playlistId: String) extends Command
  case class GetAllPlaylists() extends Command
}
