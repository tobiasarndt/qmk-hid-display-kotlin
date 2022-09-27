import kotlinx.coroutines.*

interface Monitor {
    val refreshScreenTime: Long
    val refreshDataTime: Long
    val scope: CoroutineScope
    var screen: String
    var jobs: List<Job>
    suspend fun launchMonitor() {//= coroutineScope{
        val monitor: Monitor = this
        monitor.jobs = listOf(
            scope.launch {
                while (true) {
                    updateScreen()
                    delay(monitor.refreshScreenTime)
                }
            },
            scope.launch {
                while(refreshDataTime > 0) {
                    updateData()
                    delay(monitor.refreshDataTime)
                }
            }
        )
    }

    suspend fun updateScreen()

    suspend fun updateData()

    fun terminateMonitor() {
        jobs.forEach { it.cancel("terminated monitor") }
    }
    fun title(i: Int, titleIndex: Int): String {
        // Return the character that indicates the title part from the font data
        if (i == 3) {
            return "\u00DE";
        }
        return (0x9a - titleIndex + i * 32).toChar().toString();
    }
}
