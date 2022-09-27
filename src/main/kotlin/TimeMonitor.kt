import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import java.time.LocalDateTime

class TimeMonitor (override val refreshScreenTime: Long = 10000L,
                            private val flipped: Boolean = false,
                            override val scope: CoroutineScope
) : Monitor {

    private val fileContent = this::class.java.classLoader.getResource("digits.json").readText()
    private val digitsStore: DigitsStore = Gson().fromJson(fileContent,DigitsStore::class.java)
    private val digits: List<Digit> = if (flipped) digitsStore.digitsFlipped else digitsStore.digits
    override val refreshDataTime: Long = 0
    override var screen: String = " ".repeat(84)
    override var jobs: List<Job> = listOf()
    private val bar: Char = 6.toChar()

    override suspend fun updateScreen() {
        val now: LocalDateTime = LocalDateTime.now()
        val d1: Int = now.hour / 10
        val d2: Int = now.hour % 10
        val d3: Int = now.minute / 10
        val d4: Int = now.minute % 10

        this.screen = (
            digits[d1]._0 + digits[d2]._0 + bar + digits[d3]._0 + digits[d4]._0 +
            digits[d1]._1 + digits[d2]._1 + bar + digits[d3]._1 + digits[d4]._1 +
            digits[d1]._2 + digits[d2]._2 + bar + digits[d3]._2 + digits[d4]._2 +
            digits[d1]._3 + digits[d2]._3 + bar + digits[d3]._3 + digits[d4]._3
            )
    }

    override suspend fun updateData() {
    }
}
