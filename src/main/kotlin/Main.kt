import kotlinx.coroutines.*
import org.hid4java.*
import kotlin.math.min


suspend fun main(): Unit {
    var product = "Jorne"; //"Lily58"; //
    try {
        run(product)
    } catch (e: Exception) {
        println("run failed: $e")
        main()
    }


}

suspend fun run(product: String) {
    var screenIndex: Int = 0

    var keyboard: HidDevice? = null
    var connected = false

    coroutineScope {
        launch {
            var keyboardReading: ByteArray = ByteArray(168)
            var keyboardReadingStatus: Int
            val monitorList: List<Monitor> = listOf(
                TimeMonitor(scope = this, flipped = true),
                WeatherMonitor(scope = this),
                SpotifyMonitor(scope = this)
            )
            for (monitor in monitorList) {
//                monitor.updateData()
            }
            var currentMonitor: Monitor = monitorList[screenIndex]
            //currentMonitor.launchMonitor()
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
                        screenIndex = byteArrayToInt(keyboardReading) % monitorList.size
                        println(byteArrayToInt(keyboardReading))
                        currentMonitor = monitorList[screenIndex]
                        currentMonitor.launchMonitor()
                        sendToKeyboard(keyboard!!, currentMonitor.screen)
                    }
                    delay(1000L)
                } else {
                    println("connecting to keyboard")
                    keyboard = connectKeyboard(product)
                    keyboard!!.write(byteArrayOf(1, (monitorList.size).toByte()), 2, 0)
                    connected = true
                    println("connected")
                    launch {
                        while (connected) {
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
    return result
}

fun connectKeyboard(name: String): HidDevice {
    val usage = 0x61
    val usagePage = -160
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
    println(screen)
    for (i in screen.indices) {
        screenBuffer[2 * i] = ((screen[i].code shr 8) + 2).toByte()
        screenBuffer[2 * i + 1] = (screen[i].code and 255).toByte()
    }
    for (i in 0..5) {
        keyboard.write(screenBuffer.copyOfRange(i * 28, (i + 1) * 28), 28, 0)
    }
}
