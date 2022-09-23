import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import org.hid4java.*
import org.json.JSONObject
import java.util.*

//fun main_() = runBlocking {
//    doWorld()
//    println("done")
//}
//
//suspend fun doWorld() = coroutineScope {
//    test(this)
//    launch {
//        delay(1000L)
//        print("World 1")
//    }
//    print("Hello")
//}
//
//fun test(scope: CoroutineScope) {
//    scope.launch {
//        delay(2000L)
//        println("World 2")
//    }
//}

//suspend fun main() {
//    val client = HttpClient()
//    val response: HttpResponse = client.request("https://www.yahoo.com/news/weather/germany/augsburg/augsburg-20067030")
//    val stringBody: String = response.body()
//    val temperatureRegex: Regex = """(?<="temperature":)\{[^\}]+\}""".toRegex()
//    val conditionRegex: Regex = """(?<="conditionDescription":")[^"]+""".toRegex()
//    val rainRegex: Regex = """(?<="precipitationProbability":)[^,]""".toRegex()
//    var temperatureJSON: JSONObject = JSONObject(
//        temperatureRegex.find(stringBody)?.value
//    )
//    var conditionString: String = (
//        conditionRegex.find(stringBody)?.value!!
//        )
//    var rain: String = (
//        rainRegex.find(stringBody)?.value!!
//        )
//    var weather: MutableMap<String, Any> = mutableMapOf()
//    weather["cond"] = conditionString
////    print(stringBody)
////    weather["temp"] = (Gson().fromJson(temperatureRegex.find(stringBody)?.value, weather.javaClass))
//    var test: MutableMap<String, Double> = mutableMapOf()
//    test = (Gson().fromJson(temperatureRegex.find(stringBody)?.value, test.javaClass))
//    print("0123456789".slice(0..19))
////   // val weather = mapOf("temp" to test, "cond" to conditionString, "rain" to rain)
////    println(test["high"]!! * 2)
//}


fun main(args : Array<String>): Unit = runBlocking {
    var product = "Jorne"; //"Lily58"; //
    //connectKeyboard(product)
    println("test")
    val timeMonitor: Monitor = WeatherMonitor(scope = this)

    println("test2")
    //val keyboard: HidDevice = connectKeyboard("Jorne")
    println("reading")
    var blob: ByteArray = ByteArray(168)
    var run: Boolean = true
    //sendToKeyboard(keyboard, timeMonitor.getScreen())
    coroutineScope {
        println("starting")
        timeMonitor.launchMonitor()

        launch {
            delay(30000L)
            timeMonitor.terminateMonitor()
            print("finished")
        }
//        launch {
//            while (true) {
//                println("reading")
//                println(keyboard.read(blob))
//                println(byteArrayToInt(blob))
//                println("read")
//                delay(1000L)
//            }
//        }
    }
}

fun byteArrayToInt(bytes: ByteArray): Int {
    var result = 0
    for (i in bytes.indices) {
        result = result or (bytes[i].toInt() shl 8 * i)
    }
    return result
}

fun connectKeyboard(name: String): HidDevice {
    val usage = 0x61
    val usagePage = -160
    val keyboard: HidDevice
    val hidServicesSpecification: HidServicesSpecification = HidServicesSpecification()
    hidServicesSpecification.isAutoStart = false
    val hidServices: HidServices = HidManager.getHidServices(hidServicesSpecification)

    hidServices.start()

    while (true) {
        for (hidDevice in hidServices.attachedHidDevices) {
            if (hidDevice.product == name &&
                hidDevice.usage == usage &&
                hidDevice.usagePage == usagePage
            ) {
                hidDevice.open()
                return hidDevice
            }
        }
    }
}

fun sendToKeyboard(keyboard: HidDevice, screen: String) {
    var screenBuffer: ByteArray = ByteArray(168)
    for(i in screen.indices) {
        screenBuffer[2*i] = ((screen[i].code shr 8) + 2).toByte()
        screenBuffer[2*i+1] = (screen[i].code and 255).toByte()
    }
    for(i in 0..5) {
        keyboard.write(screenBuffer.copyOfRange(i*28,(i+1)*28), 28, 0)
    }
}
