package tech.mmarca.healthcalculations.insights

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ln
import kotlin.math.sqrt
import tech.mmarca.healthcalculations.model.HrvSample
import tech.mmarca.healthcalculations.model.SleepData
import tech.mmarca.healthcalculations.stats.timeBucketedAverageOrNull

/**
 * The fewest nights a baseline rests on: the ten daily values Kiviniemi et al.
 * took their reference from (Eur J Appl Physiol 2007;101(6):743-751,
 * doi:10.1007/s00421-007-0552-2).
 */
private const val MinimumBaselineNights = 10

/**
 * A personal HRV baseline: the mean and the night-to-night standard deviation
 * of the natural log of each night's rMSSD. The log tames rMSSD's skew (Plews,
 * Laursen, Stanley, Kilding and Buchheit, Sports Med 2013;43(9):773-781,
 * doi:10.1007/s40279-013-0071-8), and the spread is each person's own instead
 * of a fixed share of the mean. Build it from the four weeks of nights before
 * the day, the baseline period of Vesterinen et al. (Med Sci Sports Exerc
 * 2016;48(7):1347-1354, doi:10.1249/MSS.0000000000000910), each night valued
 * by [nightRmssdByDate], so the baseline and the night held against it are the
 * same measure.
 */
data class HrvBaseline(
    val lnRmssdMean: Double,
    val lnRmssdSd: Double,
) {
    /** How far [rmssdMs] lies from the mean, in standard deviations of the log; null for a value that is not positive. */
    fun standardDeviationsFromMean(rmssdMs: Double): Double? {
        if (rmssdMs <= 0.0) return null
        return (ln(rmssdMs) - lnRmssdMean) / lnRmssdSd
    }

    companion object {
        /**
         * The baseline of [nightlyRmssdMs], one value a night; null with fewer than
         * [MinimumBaselineNights] positive values or no spread.
         */
        fun of(nightlyRmssdMs: Collection<Double>): HrvBaseline? {
            val logs = nightlyRmssdMs.filter { it > 0.0 }.map { ln(it) }
            if (logs.size < MinimumBaselineNights) return null
            val mean = logs.average()
            val sd = sqrt(logs.sumOf { (it - mean) * (it - mean) } / (logs.size - 1))
            // Identical nights leave rounding noise, not a spread.
            if (sd < 1e-9) return null
            return HrvBaseline(lnRmssdMean = mean, lnRmssdSd = sd)
        }
    }
}

/** A night's main sleep session and its rMSSD. */
internal data class NightRmssd(val session: SleepData, val rmssdMs: Double)

/**
 * Each night's rMSSD, by the date the night ends on: the time-weighted mean of
 * the positive readings inside the night's main sleep session (the longest
 * asleep; naps do not count). A whole night's mean is as reliable night to
 * night as the morning value and shows a hard day's load more clearly
 * (Nuuttila, Seipäjärvi, Kyröläinen and Nummela, Int J Sports Physiol Perform
 * 2022;17(8):1296-1303, doi:10.1123/ijspp.2022-0145). A night without readings
 * is left out. [hrvSamples] must cover the sessions, the hours before midnight
 * included.
 */
fun nightRmssdByDate(
    sessions: List<SleepData>,
    hrvSamples: List<HrvSample>,
    zone: ZoneId = ZoneId.systemDefault(),
): Map<LocalDate, Double> = nightsRmssd(sessions, hrvSamples, zone).mapValues { it.value.rmssdMs }

internal fun nightsRmssd(
    sessions: List<SleepData>,
    hrvSamples: List<HrvSample>,
    zone: ZoneId,
): Map<LocalDate, NightRmssd> {
    val positive = hrvSamples.filter { it.rmssdMs > 0.0 }
    if (positive.isEmpty()) return emptyMap()
    return sessions.groupBy { it.endTime.atZone(zone).toLocalDate() }
        .mapNotNull { (date, nightSessions) ->
            val session = nightSessions.mainSleepSession() ?: return@mapNotNull null
            val rmssd = positive
                .filter { !it.time.isBefore(session.startTime) && !it.time.isAfter(session.endTime) }
                .timeBucketedAverageOrNull(time = { it.time }, value = { it.rmssdMs })
                ?: return@mapNotNull null
            date to NightRmssd(session, rmssd)
        }
        .toMap()
}
