package tech.mmarca.healthcalculations.cycle

import java.time.LocalDate

/**
 * What the cycle statistics are computed from: the flow level of each day
 * (a period span from another app counts as unknown flow), the spotting days,
 * the user's exclusions, and the LH tests, temperatures and mucus.
 */
data class CycleInputs(
    val flowDays: Map<LocalDate, Int>,
    val spottingDays: Set<LocalDate> = emptySet(),
    val exclusions: Map<LocalDate, CycleExclusionReason?> = emptyMap(),
    val observations: CycleObservations = CycleObservations(),
)

/**
 * The statistics as they stood on [day]: nothing recorded after it counts,
 * so a past day does not see the future. [bleedingNone] is the journal's
 * "no bleeding" for [day].
 */
fun CycleInputs.statisticsAsOf(
    day: LocalDate,
    profile: CycleTrackingProfile = CycleTrackingProfile(),
    bleedingNone: Boolean = false,
): CycleStatistics {
    val spotting = spottingDays.filterTo(mutableSetOf()) { !it.isAfter(day) }
    return CycleCalculations.compute(
        flowDays = flowDays.filterKeys { !it.isAfter(day) },
        today = day,
        spottingDays = spotting,
        exclusions = exclusions.filterKeys { !it.isAfter(day) },
        profile = profile,
        todayBleeding = when {
            day in spotting -> DayBleeding.SPOTTING
            bleedingNone -> DayBleeding.NONE
            else -> null
        },
        observations = observations.upTo(day),
    )
}

/** These observations without the ones after [day]. */
fun CycleObservations.upTo(day: LocalDate) = CycleObservations(
    lhPositiveDays = lhPositiveDays.filterTo(mutableSetOf()) { !it.isAfter(day) },
    bbtReadings = bbtReadings.filter { !it.date.isAfter(day) },
    mucusDays = mucusDays.filterKeys { !it.isAfter(day) },
)

/**
 * The mean number of bleeding days in the last [RecentPeriodCycles] finished
 * cycles the user did not exclude. App heuristic: the window matches the one
 * the cycle length average uses. Null without a finished cycle.
 */
val CycleStatistics.averagePeriodLengthDays: Double?
    get() = cycles
        .filter { it.endDate != null && !it.isExcludedFromEstimates }
        .sortedBy { it.startDate }
        .takeLast(RecentPeriodCycles)
        .map { it.bleedingDayCount.toDouble() }
        .takeIf { it.isNotEmpty() }
        ?.average()

private const val RecentPeriodCycles = 6
