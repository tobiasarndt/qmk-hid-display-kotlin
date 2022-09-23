import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.time.LocalDateTime
import kotlin.io.path.Path

class TimeMonitor (override val refreshScreenTime: Long = 1000L,
                            private val flipped: Boolean = false,
                            override var runMonitor: Boolean = false,
                            override val scope: CoroutineScope
) : Monitor {

    private val mapper = jacksonObjectMapper()
    private val config: Map<String,List<List<String>>> =
        mapper.readValue(File(Path("src/main/resources/digits.json").toString()))
    override val refreshDataTime: Long = 0
    override var screen: String = " ".repeat(84)

    override suspend fun updateScreen() {
        val now: LocalDateTime = LocalDateTime.now()
        val d1: Int = now.hour / 10
        val d2: Int = now.hour % 10
        val d3: Int = now.minute / 10
        val d4: Int = now.minute % 10

        val bar: Char = 6.toChar()
        val digits: List<List<String>> = if (flipped) config["digits"]!! else config["digits_flipped"]!!

        this.screen = (
            digits[d1][0] + digits[d2][0] + bar + digits[d3][0] + digits[d4][0] +
            digits[d1][1] + digits[d2][1] + bar + digits[d3][1] + digits[d4][1] +
            digits[d1][2] + digits[d2][2] + bar + digits[d3][2] + digits[d4][2] +
            digits[d1][3] + digits[d2][3] + bar + digits[d3][3] + digits[d4][3]
            )
        print(screen)
    }
}
