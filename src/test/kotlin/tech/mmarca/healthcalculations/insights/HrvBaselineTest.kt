package tech.mmarca.healthcalculations.insights

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.healthcalculations.model.HrvSample
import tech.mmarca.healthcalculations.model.SleepData

class HrvBaselineTest {
    private val zone = ZoneOffset.UTC

    @Test
    fun `ten nights give the mean and spread of the log values`() {
        val nights = listOf(40.0, 45.0, 50.0, 55.0, 60.0, 50.0, 50.0, 48.0, 52.0, 47.0)
        val baseline = requireNotNull(HrvBaseline.of(nights))

        val logs = nights.map { ln(it) }
        val mean = logs.average()
        assertEquals(mean, baseline.lnRmssdMean, 1e-12)
        assertEquals(sqrt(logs.sumOf { (it - mean) * (it - mean) } / (logs.size - 1)), baseline.lnRmssdSd, 1e-12)
    }

    @Test
    fun `fewer than ten usable nights give no baseline`() {
        assertNull(HrvBaseline.of(listOf(40.0, 45.0, 50.0, 55.0, 60.0, 50.0, 50.0, 48.0, 52.0, 0.0)))
    }

    @Test
    fun `nights without spread give no baseline`() {
        assertNull(HrvBaseline.of(List(12) { 50.0 }))
    }

    @Test
    fun `a value one standard deviation below the mean reads minus one`() {
        val baseline = HrvBaseline(lnRmssdMean = ln(50.0), lnRmssdSd = 0.2)

        assertEquals(-1.0, baseline.standardDeviationsFromMean(50.0 * exp(-0.2))!!, 1e-12)
        assertNull(baseline.standardDeviationsFromMean(0.0))
    }

    @Test
    fun `a night is the mean inside its main session, dated by its end, the hours before midnight included`() {
        val night = session("night", "2026-11-09T22:00:00Z", "2026-11-10T06:00:00Z")
        val nap = session("nap", "2026-11-10T14:00:00Z", "2026-11-10T14:30:00Z")
        val samples = listOf(
            sample("2026-11-09T23:00:00Z", 40.0),
            sample("2026-11-10T03:00:00Z", 60.0),
            // In the nap, which does not count, and between the two.
            sample("2026-11-10T14:10:00Z", 10.0),
            sample("2026-11-10T10:00:00Z", 10.0),
            sample("2026-11-10T04:00:00Z", 0.0),
        )

        val byDate = nightRmssdByDate(listOf(night, nap), samples, zone)

        assertEquals(setOf(LocalDate.parse("2026-11-10")), byDate.keys)
        assertEquals(50.0, byDate.getValue(LocalDate.parse("2026-11-10")), 1e-9)
    }

    @Test
    fun `a night without readings is left out`() {
        val night = session("night", "2026-11-09T22:00:00Z", "2026-11-10T06:00:00Z")
        assertEquals(emptyMap<LocalDate, Double>(), nightRmssdByDate(listOf(night), listOf(sample("2026-11-10T12:00:00Z", 50.0)), zone))
    }

    private fun session(id: String, start: String, end: String): SleepData {
        val from = Instant.parse(start)
        val to = Instant.parse(end)
        return SleepData(id = id, startTime = from, endTime = to, durationMs = Duration.between(from, to).toMillis(), source = "test")
    }

    private fun sample(time: String, rmssdMs: Double) = HrvSample(time = Instant.parse(time), rmssdMs = rmssdMs, source = "test")
}
