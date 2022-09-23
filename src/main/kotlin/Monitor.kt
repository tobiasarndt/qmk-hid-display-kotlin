import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface Monitor {
    val refreshScreenTime: Long
    val refreshDataTime: Long
    val scope: CoroutineScope
    var screen: String
    var runMonitor: Boolean
    suspend fun updateScreen()
    suspend fun launchMonitor(monitor: Monitor = this) {//= coroutineScope{
        monitor.runMonitor = true
        scope.launch {
            while (monitor.runMonitor) {
                updateScreen()
                delay(monitor.refreshScreenTime)
            }
        }
        scope.launch {
            updateData()
        }
    }
    suspend fun updateData() {

    }
    fun terminateMonitor() {
        runMonitor = false
    }
    fun title(i: Int, titleIndex: Int): String {
        // Return the character that indicates the title part from the font data
        if (i === 3) {
            return "\u00DE";
        }
        return (0x9a - titleIndex + i * 32).toChar().toString();
    }
}
