import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class WeatherMonitor(override val refreshScreenTime: Long = 5000L,
                     override val refreshDataTime: Long = 60000L,
                     private val flipped: Boolean = false,
                     override var runMonitor: Boolean = false,
                     override val scope: CoroutineScope
) : Monitor {

    override var screen: String = " ".repeat(84)
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
    private var lastScreenIndex: Int = 0

    override suspend fun updateData() {
        scope.launch {
            val response: HttpResponse = (
                client.request("https://www.yahoo.com/news/weather/germany/augsburg/augsburg-20067030")
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
            delay(refreshDataTime)
        }
    }

    override suspend fun updateScreen() {
        if(!initialized) {
            updateData()
            initialized = true
        } else {
            var conditionSliced: String = condition
            val tempNowString: Int = ((temperature["now"]!! - 32)*5/9).roundToInt()
            val tempHighString: Int = ((temperature["high"]!! - 32)*5/9).roundToInt()
            val tempLowString: Int = ((temperature["low"]!! - 32)*5/9).roundToInt()
            val tempHLString = "${30.toChar()}$tempHighString ${31.toChar()}$tempLowString"
            if(lastWeather["condition"] == condition &&
                condition.length > 9) {
                lastScreenIndex = (lastScreenIndex + 1) % condition.length
                conditionSliced = condition.slice(
                    lastScreenIndex..min(lastScreenIndex + 9, condition.length)
                )
            } else {
                conditionSliced = condition.slice(0..min(condition.length-1, 9))
            }
            lastWeather["condition"] = condition

            screen =
                "desc: $conditionSliced${" ".repeat(max(0, 9 - (" $conditionSliced").length))}" +
                " |  ${title(0,2)}" +
                "temp: ${tempNowString}${" ".repeat(max(0, 9 - (" $tempNowString").length))}" +
                " |  ${title(1,2)}" +
                "temp:${tempHLString}${" ".repeat(max(0, 10 - (" $tempHLString").length))}" +
                "|  ${title(1,2)}" +
                "rain: ${rain}%${" ".repeat(max(0, 8 - ("" + rain).length))} |  ${title(3, 2)}"
        }
    }
}
