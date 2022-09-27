import com.adamratzman.spotify.SpotifyException
import com.adamratzman.spotify.models.*

data class TrackInfo(
    val name: String,
    val artist: String,
    val album: String,
    val durationMs: Int?)

@JvmName("primarySimpleArtist")
fun List<SimpleArtist>.primary() = this.firstOrNull()?.name ?: "Unknown"
fun List<SimpleLocalArtist>.primary() = this.firstOrNull()?.name ?: "Unknown"

fun Playable.trackInfo(): TrackInfo = when(this){
    is LocalTrack -> TrackInfo(name, artists.primary(), album.name, durationMs)
    is PodcastEpisodeTrack -> TrackInfo(name, artists.primary(), album.name, durationMs)
    is Track -> TrackInfo(name, artists.primary(), album.name, durationMs).also { println("Hihi.") }
    is Episode -> TrackInfo(name, "Unknown", "Unknown", durationMs)
    else -> throw SpotifyException.ParseException("failed to parse playable uri to playable object")
}


