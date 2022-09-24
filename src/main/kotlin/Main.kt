import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import org.hid4java.*
import org.json.JSONObject
import java.util.*
import kotlin.math.min

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


suspend fun main(args : Array<String>): Unit {
    var product = "Jorne"; //"Lily58"; //
    //sendToKeyboard(keyboard, timeMonitor.getScreen())

    var screenIndex: Int = 0

    var keyboard: HidDevice? = null
    var connected = false


    coroutineScope {
        launch {
            var keyboardReading: ByteArray = ByteArray(168)
            var keyboardReadingStatus: Int
            val monitorList: List<Monitor> = listOf(
                TimeMonitor(scope = this),
                WeatherMonitor(scope = this)
            )
            for (monitor in monitorList) {
                monitor.updateData()
            }
            var currentMonitor: Monitor = monitorList[screenIndex]
            currentMonitor.launchMonitor()
            while (true) {
                if (connected) {
                    println("waiting for keyboard to send new index")
                    keyboardReadingStatus = keyboard!!.read(keyboardReading)
                    println("new index received")
                    if (keyboardReadingStatus == -1) {
                        currentMonitor.terminateMonitor()
                        connected = false
                    } else {
                        println("changing screen")
                        currentMonitor.terminateMonitor()
                        screenIndex = byteArrayToInt(keyboardReading) % 2
                        currentMonitor = monitorList[screenIndex]
                        currentMonitor.launchMonitor()
                        sendToKeyboard(keyboard!!, currentMonitor.screen)
                    }
                    delay(1000L)
                } else {
                    keyboard = connectKeyboard(product)
                    connected = true
                    launch {
                        while (connected) {
                            println(currentMonitor.jobs)
                            println(currentMonitor.screen)
                            sendToKeyboard(keyboard!!, currentMonitor.screen)
                            delay(min(currentMonitor.refreshScreenTime, 30000L))
                        }
                    }
                }
            }
        }


    }
}

fun byteArrayToInt(bytes: ByteArray): Int {
    var result = 0
    for (i in bytes.indices) {
        result = result or (bytes[i].toInt() shl 8 * i)
    }
    println(result)
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
