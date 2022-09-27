import com.adamratzman.spotify.*
import com.adamratzman.spotify.models.*
import com.google.gson.Gson
import io.javalin.Javalin
import io.ktor.client.network.sockets.*
import kotlinx.coroutines.*
import java.io.FileWriter
import java.lang.Math.*
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class SpotifyMonitor(
    override val refreshScreenTime: Long = 1000L,
    override val refreshDataTime: Long = 1000L,
    override val scope: CoroutineScope
) : Monitor {
    override var screen: String = " ".repeat(84)

    private var lastSong: MutableMap<String, String> = mutableMapOf(
        "name" to "",
        "artist" to ""
    )
    private var scrollingIndex: Int = 0
    private val bar = 6.toChar()
    private val prog1 = 12.toChar().toString()
    private val prog0 = 11.toChar().toString()
    private val progL = 17.toChar()
    private val progR = 16.toChar()

    override var jobs: List<Job> = listOf()
    private lateinit var api: SpotifyClientApi
    private var currentlyPlaying: CurrentlyPlayingObject? = null
    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private val codeVerifier = (1..50)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")

    var initializingApi: Boolean = false

    init {
        this.let { that ->
            scope.launch {
                readSpotifyApi()
                while (true) {
                    if (that::api.isInitialized) {
                        delay(10000L)
                        try {
                            api.refreshToken()
                        } catch (e: SpotifyException.BadRequestException) {
                            initializeSpotifyApi()
                        }
                        writeApi()
                        delay(3000000L)
                    }
                }
            }
        }
    }

    data class ApiStore(
        val clientId: String? = null,
        val redirectUri: String? = null,
        val token: Token? = null,
        val refreshToken: String? = null,
        val codeVerifier: String? = null
    )

    private suspend fun writeApi() =
        api.refreshToken().also {
            ApiStore(
                api.clientId,
                api.redirectUri,
                api.token,
                api.token.refreshToken,
                codeVerifier
            ).let { apiStore ->
                runCatching {
                    withContext(Dispatchers.IO) { FileWriter(".spotifyApi").use { Gson().toJson(apiStore, it) } }
                }
            }
        }

    private suspend fun readSpotifyApi() {
        try {
            val apiStore = Gson().fromJson(Files.newBufferedReader(Paths.get(".spotifyApi")), ApiStore::class.java)
            val spotifyUserAuthorization = SpotifyUserAuthorization(
                token = apiStore.token,
                refreshTokenString = apiStore.refreshToken,
                pkceCodeVerifier = apiStore.codeVerifier
            )
            api = spotifyClientPkceApi(
                clientId = apiStore.clientId,
                redirectUri = apiStore.redirectUri,
                authorization = spotifyUserAuthorization
            ).build()
        } catch (e: SpotifyException.BadRequestException) {
            initializeSpotifyApi()
        } catch (e: FileSystemNotFoundException) {
            initializeSpotifyApi()
        } catch (e: NoSuchFileException) {
            initializeSpotifyApi()
        }
    }


    private suspend fun initializeSpotifyApi() {

        if (!initializingApi) {
            initializingApi = true
            val codeChallenge = getSpotifyPkceCodeChallenge(codeVerifier) // helper method
            val url: String = getSpotifyPkceAuthorizationUrl(
                SpotifyScope.USER_READ_CURRENTLY_PLAYING,
                SpotifyScope.USER_READ_PLAYBACK_STATE,

                clientId = "9bfb1b45da0d4c629bb7147b81532fde",
                redirectUri = "http://localhost:8880/callback/",
                codeChallenge = codeChallenge
            )
            println(url)
            var code: String? = ""
            val app = Javalin.create().start(8880)
            app.get("/callback/*") { ctx -> // the {} syntax does not allow slashes ('/') as part of the parameter
                code = (ctx.queryParam("code"))
            }
            while (true) {
                if (code != "") {
                    app.close()
                    app.stop()
                    api = spotifyClientPkceApi(
                        "9bfb1b45da0d4c629bb7147b81532fde", // optional. include for token refresh
                        "http://localhost:8880/callback/", // optional. include for token refresh
                        code!!,
                        codeVerifier // the same code verifier you used to generate the code challenge
                    ) {
                        retryWhenRateLimited = false
                    }.build()
                    writeApi()
                    initializingApi = false
                }
                delay(1000L)
            }
        }
    }

    override suspend fun updateScreen() {
        fun Int?.getStringFromMs(): String = this?.toLong()?.let {
            with(TimeUnit.MILLISECONDS) {
                val hours = toHours(it)
                val minutes = toMinutes(it) % 60
                val seconds = toSeconds(it) % 60
                DecimalFormat("00").run {
                    if (hours > 0)
                        "${hours.toString()}:${format(minutes)}:${format(seconds)}"
                    else
                        "${minutes}:${format(seconds)}"
                }
            }
        } ?: "???"

        val trackInfo = currentlyPlaying?.item?.trackInfo()
        val duration: String = trackInfo?.durationMs.getStringFromMs()
        val progress: String = currentlyPlaying?.progressMs.getStringFromMs()
        val progressRelative: Int = ((currentlyPlaying?.progressMs?.toDouble() ?: 0.0) /
            (trackInfo?.durationMs?.toDouble() ?: Double.POSITIVE_INFINITY) * 17).roundToInt()
        val isPlaying: Boolean = currentlyPlaying?.isPlaying ?: false
        var name: String = trackInfo?.name ?: "Unknown"
        var artist: String = trackInfo?.artist ?: "Unknown"
        var centerName = true
        var centerArtist = true
        if ((lastSong["name"] == name) &&
            (lastSong["artist"] == artist) &&
            (max(name.length, artist.length) > 19)
        ) {
            if (name.length > 19 && scrollingIndex < name.length) {
                name = name.slice(scrollingIndex..min(scrollingIndex + 18, name.length - 1))
                centerName = false
            }
            if (artist.length > 19 && scrollingIndex < artist.length) {
                artist = artist.slice(scrollingIndex..min(scrollingIndex + 18, artist.length - 1))
                centerArtist = false
            }
            if (!centerName || !centerArtist) {
                scrollingIndex = (scrollingIndex + 1) % max(name.length, artist.length)
            }
        } else {
            lastSong["name"] = name
            lastSong["artist"] = artist
            name = name.slice(0..min(18, name.length - 1))
            artist = artist.slice(0..min(18, artist.length - 1))
        }
        val nameLine = (
            (if (centerName) (" ".repeat((19 - name.length) / 2) + name + " ".repeat((20 - name.length) / 2))
            else (name + " ".repeat(19 - name.length))) + bar + title(0, 3)
            )
        val artistLine = (
            (if (centerArtist) (" ".repeat((19 - artist.length) / 2) + artist + " ".repeat((20 - artist.length) / 2))
            else (artist + " ".repeat(19 - artist.length))) + bar + title(0, 3)
            )
        val progressLine = (
            progress + " ".repeat(9 - progress.length) + (if (isPlaying) 5.toChar() else 4.toChar()) +
                " ".repeat(9 - duration.length) + duration + bar + title(2, 3)
            )
        val progressBarLine = (progL + prog1.repeat(progressRelative) +
            prog0.repeat(17 - progressRelative) + progR + bar +
            title(3, 3))
        screen = nameLine + artistLine + progressLine + progressBarLine
    }

    override suspend fun updateData() {
        if (this::api.isInitialized && !initializingApi) {
            try {
                currentlyPlaying = withTimeout(500L) {
                    api.player.getCurrentlyPlayingRestAction().complete()
                }
            } catch (e: SpotifyException.BadRequestException) {
                println("Bad Credentials: $e")
                initializeSpotifyApi()
            } catch (e: ConnectTimeoutException) {
                println("Spotify currentlyPlaying Connection Timed out")
            }
        }
    }
}
