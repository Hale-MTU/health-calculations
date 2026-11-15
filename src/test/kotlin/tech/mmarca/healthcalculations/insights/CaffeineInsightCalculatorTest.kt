package tech.mmarca.healthcalculations.insights

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.healthcalculations.model.CaffeineCatalogMatch
import tech.mmarca.healthcalculations.model.CaffeineEntry
import tech.mmarca.healthcalculations.model.CaffeineSourceCategory
import tech.mmarca.healthcalculations.model.DatePeriod
import tech.mmarca.healthcalculations.preferences.CaffeinePreferences

class CaffeineInsightCalculatorTest {

    private val preferences = CaffeinePreferences()
    private val entry = CaffeineEntry(
        id = "coffee-1",
        startTime = Instant.parse("2026-07-01T08:00:00Z"),
        endTime = Instant.parse("2026-07-01T08:10:00Z"),
        caffeineMg = 100.0,
        name = "Coffee",
        source = "test.source",
        mealType = 0,
    )

    @Test
    fun `contribution is zero before intake and positive after absorption`() {
        assertEquals(
            0.0,
            CaffeineInsightCalculator.contributionMg(
                entry = entry,
                at = Instant.parse("2026-07-01T07:59:00Z"),
                preferences = preferences,
            ),
            0.001,
        )

        val contribution = CaffeineInsightCalculator.contributionMg(
            entry = entry,
            at = Instant.parse("2026-07-01T09:00:00Z"),
            preferences = preferences,
        )

        assertTrue(contribution > 0.0)
    }

    @Test
    fun `a drink sipped over two hours peaks later and lower than one taken at once`() {
        // The same 160 mg downed at once or nursed over two hours: the interval spreads the dose.
        val atOnce = entry.copy(
            caffeineMg = 160.0,
            endTime = entry.startTime.plusSeconds(60),
        )
        val slow = entry.copy(
            caffeineMg = 160.0,
            endTime = entry.startTime.plusSeconds(2 * 60 * 60),
        )

        val atOncePeak = CaffeineInsightCalculator.peakContribution(atOnce, preferences)
        val slowPeak = CaffeineInsightCalculator.peakContribution(slow, preferences)

        assertTrue(slowPeak.time.isAfter(atOncePeak.time))
        assertTrue(slowPeak.valueMg < atOncePeak.valueMg)
        // Half an hour in, only a quarter of the slow drink has even been swallowed.
        val halfHour = entry.startTime.plusSeconds(30 * 60)
        val atOnceHalfHour = CaffeineInsightCalculator.contributionMg(atOnce, halfHour, preferences)
        val slowHalfHour = CaffeineInsightCalculator.contributionMg(slow, halfHour, preferences)
        assertTrue(slowHalfHour < atOnceHalfHour / 2)
    }

    @Test
    fun `active caffeine decays over time`() {
        val early = CaffeineInsightCalculator.activeCaffeineMg(
            entries = listOf(entry),
            at = Instant.parse("2026-07-01T09:00:00Z"),
            preferences = preferences,
        )
        val late = CaffeineInsightCalculator.activeCaffeineMg(
            entries = listOf(entry),
            at = Instant.parse("2026-07-01T20:00:00Z"),
            preferences = preferences,
        )

        assertTrue(early > late)
    }

    @Test
    fun `a bedtime before noon belongs to the night that ENDS the day`() {
        val insights = CaffeineInsightCalculator.build(
            entries = listOf(entry),
            period = DatePeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1)),
            preferences = preferences.copy(bedtime = LocalTime.of(6, 30)),
            now = Instant.parse("2026-07-03T12:00:00Z"),
            zone = ZoneOffset.UTC,
            catalog = NoDrinkCatalog,
        )

        // July 1's 06:30 bedtime is the morning of July 2. Anchored to July 1 it sat before the coffee.
        val expected = CaffeineInsightCalculator.activeCaffeineMg(
            entries = listOf(entry),
            at = Instant.parse("2026-07-02T06:30:00Z"),
            preferences = preferences,
        )
        assertEquals(expected, insights.dailyStats.single().bedtimeMg, 0.001)
        assertTrue(insights.dailyStats.single().bedtimeMg > 0.0)
    }

    @Test
    fun `an evening bedtime stays on its own date`() {
        val insights = CaffeineInsightCalculator.build(
            entries = listOf(entry),
            period = DatePeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1)),
            preferences = preferences.copy(bedtime = LocalTime.of(22, 30)),
            now = Instant.parse("2026-07-03T12:00:00Z"),
            zone = ZoneOffset.UTC,
            catalog = NoDrinkCatalog,
        )

        val expected = CaffeineInsightCalculator.activeCaffeineMg(
            entries = listOf(entry),
            at = Instant.parse("2026-07-01T22:30:00Z"),
            preferences = preferences,
        )
        assertEquals(expected, insights.dailyStats.single().bedtimeMg, 0.001)
    }

    @Test
    fun `nights that have not been lived yet are not counted`() {
        val insights = CaffeineInsightCalculator.build(
            entries = listOf(entry),
            period = DatePeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3)),
            preferences = preferences,
            now = Instant.parse("2026-07-02T12:00:00Z"),
            zone = ZoneOffset.UTC,
            catalog = NoDrinkCatalog,
        )

        // Only July 1's 22:30 has passed; July 2's and July 3's are projections.
        assertEquals(listOf(true, false, false), insights.dailyStats.map { it.nightCompleted })
        assertEquals(1, insights.totalNights)
        assertEquals(1, insights.safeNights)
    }

    @Test
    fun `an unlived night neither breaks nor extends the safe sleep streak`() {
        val yesterday = Instant.parse("2026-07-01T08:00:00Z")
        val today = Instant.parse("2026-07-02T08:00:00Z")
        val entries = listOf(
            entry.copy(id = "small", startTime = yesterday, endTime = yesterday.plusSeconds(600), caffeineMg = 20.0),
            entry.copy(id = "huge", startTime = today, endTime = today.plusSeconds(600), caffeineMg = 900.0),
        )

        val insights = CaffeineInsightCalculator.build(
            entries = entries,
            period = DatePeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)),
            preferences = preferences,
            now = Instant.parse("2026-07-02T12:00:00Z"),
            zone = ZoneOffset.UTC,
            catalog = NoDrinkCatalog,
        )

        // Tonight is projected unsafe but has not happened: the streak counts from the last night lived.
        assertEquals(false, insights.dailyStats.last().safeForSleep)
        assertEquals(false, insights.dailyStats.last().nightCompleted)
        assertEquals(1, insights.safeSleepStreak)
    }

    @Test
    fun `the curve covers the period's last day, midnight to midnight`() {
        val insights = CaffeineInsightCalculator.build(
            entries = listOf(entry),
            period = DatePeriod(LocalDate.of(2026, 6, 28), LocalDate.of(2026, 7, 1)),
            preferences = preferences.copy(bedtime = LocalTime.of(22, 30)),
            now = Instant.parse("2026-07-03T12:00:00Z"),
            zone = ZoneOffset.UTC,
            catalog = NoDrinkCatalog,
        )

        // A past day is drawn whole, not the hours around "now".
        assertEquals(Instant.parse("2026-07-01T00:00:00Z"), insights.curvePoints.first().time)
        assertEquals(Instant.parse("2026-07-02T00:00:00Z"), insights.curvePoints.last().time)
        // The bedtime figure is that day's night.
        val expected = CaffeineInsightCalculator.activeCaffeineMg(
            entries = listOf(entry),
            at = Instant.parse("2026-07-01T22:30:00Z"),
            preferences = preferences,
        )
        assertEquals(expected, insights.bedtimeMg, 0.001)
    }

    @Test
    fun `a bedtime after midnight stretches the curve to reach it`() {
        val insights = CaffeineInsightCalculator.build(
            entries = listOf(entry),
            period = DatePeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1)),
            preferences = preferences.copy(bedtime = LocalTime.of(1, 30)),
            now = Instant.parse("2026-07-03T12:00:00Z"),
            zone = ZoneOffset.UTC,
            catalog = NoDrinkCatalog,
        )

        assertEquals(Instant.parse("2026-07-02T01:30:00Z"), insights.curvePoints.last().time)
    }

    @Test
    fun `a drink older than a week adds nothing`() {
        val weekOld = entry.copy(
            startTime = Instant.parse("2026-06-23T08:00:00Z"),
            endTime = Instant.parse("2026-06-23T08:10:00Z"),
            caffeineMg = 10_000.0,
        )

        // The slowest half-life allowed: 16 half-lives on, the model alone still leaves about 0.15 mg.
        val activeMg = CaffeineInsightCalculator.activeCaffeineMg(
            entries = listOf(weekOld),
            at = Instant.parse("2026-07-01T08:00:01Z"),
            preferences = preferences.copy(halfLifeMinutes = CaffeinePreferences.MaxHalfLifeMinutes),
        )

        assertEquals(0.0, activeMg, 0.0)
    }

    @Test
    fun `a night that shows as the threshold counts as safe, and needs no wait`() {
        val threshold = preferences.sleepThresholdMg
        // Just under half a milligram over: it shows as the threshold itself.
        assertFalse(CaffeineInsightCalculator.isOverSleepThreshold(threshold + 0.49, threshold))
        assertTrue(CaffeineInsightCalculator.isOverSleepThreshold(threshold + 0.5, threshold))

        // A dose sized so the level at 22:30 lands between the threshold and half a milligram over it.
        val bedtime = Instant.parse("2026-07-01T22:30:00Z")
        val unitDose = CaffeineInsightCalculator.activeCaffeineMg(listOf(entry.copy(caffeineMg = 1.0)), bedtime, preferences)
        val dose = entry.copy(caffeineMg = (threshold + 0.25) / unitDose)
        val insights = CaffeineInsightCalculator.build(
            entries = listOf(dose),
            period = DatePeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1)),
            preferences = preferences,
            now = bedtime,
            zone = ZoneOffset.UTC,
            catalog = NoDrinkCatalog,
        )

        assertTrue(insights.bedtimeMg > threshold)
        assertTrue(insights.dailyStats.single().safeForSleep)
        assertEquals(0L, insights.timeToThresholdMinutes)
    }
}

/** A catalog with no drinks: entries keep the generic category of their name. */
private object NoDrinkCatalog : CaffeineCatalog {
    override fun matchName(name: String?): CaffeineCatalogMatch? = null

    override fun inferGenericCategory(name: String?): CaffeineSourceCategory = CaffeineSourceCategory.OTHER
}
