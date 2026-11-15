package tech.mmarca.healthcalculations.insights

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.healthcalculations.model.ExerciseRoutePoint
import tech.mmarca.healthcalculations.model.ExerciseSessionRecord
import tech.mmarca.healthcalculations.model.HeartRateSample

class RouteVo2MaxTest {
    private val start = Instant.parse("2026-11-15T08:00:00Z")
    private val whole = listOf(CardioLoadTimeWindow(start, start.plusSeconds(60 * 60)))

    /** Northwards along a meridian, one fix a minute, so each minute is exactly [metresPerMinute]. */
    private fun route(minutes: Int, metresPerMinute: Double): List<ExerciseRoutePoint> {
        val degreesPerMetre = 180.0 / (Math.PI * 6_371_000.0)
        return (0..minutes).map { minute ->
            ExerciseRoutePoint(start.plusSeconds(minute * 60L), minute * metresPerMinute * degreesPerMetre, 11.0, null, null, null)
        }
    }

    private fun steady(minutes: Int, bpm: Long) =
        (0..minutes * 60).map { second -> HeartRateSample(start.plusSeconds(second.toLong()), bpm, "test") }

    @Test
    fun `a steady run divides the ACSM running cost by the reserve`() {
        // 200 m/min, reserve (134 - 50) / (190 - 50) = 0.6: 0.2 x 200 / 0.6 + 3.5.
        val vo2 = estimateRouteVo2Max(
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, route(6, 200.0), steady(6, 134), whole, 50, 190,
        )
        assertEquals(70.1667, vo2!!, 1e-4)
    }

    @Test
    fun `a hike at walking pace takes the walking cost`() {
        // 80 m/min, reserve (119 - 50) / 140: 0.1 x 80 / reserve + 3.5.
        val vo2 = estimateRouteVo2Max(
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING, route(6, 80.0), steady(6, 119), whole, 50, 190,
        )
        assertEquals(19.7319, vo2!!, 1e-4)
    }

    @Test
    fun `another sport, too few minutes or minutes outside the reserve give none`() {
        assertNull(estimateRouteVo2Max(ExerciseSessionRecord.EXERCISE_TYPE_BIKING, route(6, 200.0), steady(6, 134), whole, 50, 190))
        assertNull(estimateRouteVo2Max(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, route(4, 200.0), steady(4, 134), whole, 50, 190))
        // 60 bpm is 7 % of the reserve: below 40 %, every minute is left out.
        assertNull(estimateRouteVo2Max(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, route(6, 200.0), steady(6, 60), whole, 50, 190))
    }

    @Test
    fun `a minute that reaches into a pause does not count`() {
        // Active 0-90 s and 210-420 s: only minutes 0-1, 4-5, 5-6 and 6-7 lie inside, four of eight.
        val windows = listOf(
            CardioLoadTimeWindow(start, start.plusSeconds(90)),
            CardioLoadTimeWindow(start.plusSeconds(210), start.plusSeconds(420)),
        )
        assertNull(estimateRouteVo2Max(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, route(8, 200.0), steady(8, 134), windows, 50, 190))
        assertEquals(
            70.1667,
            estimateRouteVo2Max(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, route(8, 200.0), steady(8, 134), whole, 50, 190)!!,
            1e-4,
        )
    }
}
