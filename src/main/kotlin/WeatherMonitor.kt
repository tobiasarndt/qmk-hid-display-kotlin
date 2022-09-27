import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import java.lang.Math.max
import java.lang.Math.min
import kotlin.math.roundToInt

//import kotlin.math.max
//import kotlin.math.min
//import kotlin.math.roundToInt

class WeatherMonitor(override val refreshScreenTime: Long = 1000L,
                     override val refreshDataTime: Long = 60000L,
                     override val scope: CoroutineScope
) : Monitor {

    override var screen: String = " ".repeat(84)
    override var jobs: List<Job> = listOf()
    private var initialized: Boolean = false

    private val client = HttpClient()
    private val temperatureRegex: Regex = """(?<="temperature":)\{[^\}]+\}""".toRegex()
    private val conditionRegex: Regex = """(?<="conditionDescription":")[^"]+""".toRegex()
    private val rainRegex: Regex = """(?<="precipitationProbability":)[^,]""".toRegex()

    private var temperature: MutableMap<String, Double> = mutableMapOf(
        "now" to 0.0,
        "high" to 0.0,
        "low" to 0.0,
        "feelslike" to -1.0
    )
    private var condition: String = ""
    private var rain: String = ""

    private var lastWeather: MutableMap<String, String> = mutableMapOf(
        "condition" to condition
    )
    private var lastScrollingIndex: Int = 0

    override suspend fun updateData() {
        try {
            val response: HttpResponse = (
                client.request("https://de.nachrichten.yahoo.com/wetter/")
                )
            val stringBody: String = response.body()
            temperature = Gson().fromJson(
                temperatureRegex.find(stringBody)?.value, temperature.javaClass
            )
            condition = (
                conditionRegex.find(stringBody)?.value!!
                )
            rain = (
                rainRegex.find(stringBody)?.value!!
                )
            initialized = true
            println("----------------------test-----------------")
            updateScreen()
        } catch (e: Exception) {
            println("updateWeather failed: $e")
        }
    }

    override suspend fun updateScreen() {
        if(!initialized) {
            initialized = true
        } else {
            var conditionSliced: String
            val tempNowString: Int = ((temperature["now"]!!)).roundToInt()
            val tempHighString: Int = ((temperature["high"]!!)).roundToInt()
            val tempLowString: Int = ((temperature["low"]!!)).roundToInt()
            val tempHLString = "${30.toChar()}$tempHighString ${31.toChar()}$tempLowString"
            if(lastWeather["condition"] == condition &&
                condition.length > 11) {
                conditionSliced = condition.slice(
                    lastScrollingIndex..min(lastScrollingIndex + 11, condition.length - 1)
                )
                lastScrollingIndex = (lastScrollingIndex + 1) % condition.length
            } else {
                conditionSliced = condition.slice(0..min(condition.length-1, 11))
            }
            lastWeather["condition"] = condition

            screen =
                "desc: $conditionSliced${" ".repeat(max(0, 13 - (" $conditionSliced").length))}" +
                " ${6.toChar()}${title(0,2)}" +
                "temp: ${tempNowString}${" ".repeat(max(0, 13 - (" $tempNowString").length))}" +
                " ${6.toChar()}${title(1,2)}" +
                "temp: ${tempHLString}${" ".repeat(max(0, 13 - (" $tempHLString").length))}" +
                " ${6.toChar()}${title(1,2)}" +
                "rain: ${rain}%${" ".repeat(max(0, 11 - ("" + rain).length))} ${6.toChar()}${title(3, 2)}"
        }
    }
}
