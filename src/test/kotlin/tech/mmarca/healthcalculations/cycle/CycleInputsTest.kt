package tech.mmarca.healthcalculations.cycle

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.healthcalculations.model.CycleRecordValues

class CycleInputsTest {

    private fun day(n: Int): LocalDate = LocalDate.of(2026, 1, 1).plusDays((n - 1).toLong())

    /** A period of [length] days starting on each of [starts]. */
    private fun inputs(vararg starts: Int, length: Int = 4, spotting: Set<LocalDate> = emptySet()) = CycleInputs(
        flowDays = starts.flatMap { start -> (0 until length).map { day(start + it) } }
            .associateWith { CycleRecordValues.FLOW_MEDIUM },
        spottingDays = spotting,
    )

    @Test
    fun `a past day does not see what was recorded after it`() {
        val inputs = inputs(1, 29, 57)

        val past = inputs.statisticsAsOf(day(35))

        assertEquals(listOf(day(1), day(29)), past.cycleStarts)
        assertEquals(7, past.currentCycleDay)
        assertEquals(
            CycleCalculations.compute(inputs.flowDays.filterKeys { !it.isAfter(day(35)) }, today = day(35)),
            past,
        )
    }

    @Test
    fun `the last day gives the same statistics as computing them directly`() {
        val inputs = inputs(1, 29, 57)

        assertEquals(CycleCalculations.compute(inputs.flowDays, today = day(60)), inputs.statisticsAsOf(day(60)))
    }

    @Test
    fun `spotting on the day wins over the journal's no bleeding`() {
        val inputs = inputs(1, 29, spotting = setOf(day(40)))

        val spotting = inputs.statisticsAsOf(day(40), bleedingNone = true)

        assertEquals(
            CycleCalculations.compute(
                inputs.flowDays,
                today = day(40),
                spottingDays = setOf(day(40)),
                todayBleeding = DayBleeding.SPOTTING,
            ),
            spotting,
        )
    }

    @Test
    fun `observations after the day are left out`() {
        val observations = CycleObservations(
            lhPositiveDays = setOf(day(12), day(41)),
            bbtReadings = listOf(BbtReading(day(10), 36.4), BbtReading(day(42), 36.8)),
            mucusDays = mapOf(day(11) to true, day(43) to true),
        )

        val upTo = observations.upTo(day(41))

        assertEquals(setOf(day(12), day(41)), upTo.lhPositiveDays)
        assertEquals(listOf(day(10)), upTo.bbtReadings.map { it.date })
        assertEquals(setOf(day(11)), upTo.mucusDays.keys)
    }

    @Test
    fun `the period average counts finished cycles only`() {
        val threeDays = inputs(1, 29, length = 3).flowDays
        val current = (0 until 7).associate { day(57 + it) to CycleRecordValues.FLOW_MEDIUM }

        val stats = CycleInputs(flowDays = threeDays + current).statisticsAsOf(day(63))

        assertEquals(3.0, stats.averagePeriodLengthDays!!, 1e-9)
    }

    @Test
    fun `no finished cycle gives no period average`() {
        assertNull(inputs(1).statisticsAsOf(day(3)).averagePeriodLengthDays)
        assertNull(CycleStatistics().averagePeriodLengthDays)
    }
}
