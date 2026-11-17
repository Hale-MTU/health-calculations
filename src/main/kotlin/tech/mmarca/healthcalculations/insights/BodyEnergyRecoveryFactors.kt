package tech.mmarca.healthcalculations.insights

import java.time.Duration

/**
 * The HRV factor of each sleep session's charge, by session id: that of the
 * night ([nightsRmssd]) the session's middle lies in. A nap, or a night without
 * a value, has none and charges unscaled.
 */
internal fun hrvChargeMultipliersBySession(inputs: BodyEnergyTimelineInputs): Map<String, Double> {
    val nights = nightsRmssd(inputs.sleepSessions, inputs.hrvSamples, inputs.zone).values
    if (nights.isEmpty()) return emptyMap()
    return inputs.sleepSessions.mapNotNull { session ->
        val middle = session.startTime.plus(Duration.between(session.startTime, session.endTime).dividedBy(2))
        val night = nights.firstOrNull { !middle.isBefore(it.night.startTime) && !middle.isAfter(it.night.endTime) }
            ?: return@mapNotNull null
        session.id to hrvChargeMultiplier(inputs.hrvBaseline, night.rmssdMs)
    }.toMap()
}

internal data class RespiratoryFactor(
    val drainMultiplier: Double,
    val chargePenalty: Double,
)

/**
 * How a night's HRV ([nightRmssdByDate]) scales its charge. A night more than
 * one standard deviation below the baseline ([HrvBaseline]) is a meaningful
 * drop, the single-day criterion of Kiviniemi et al. (Eur J Appl Physiol
 * 2007;101(6):743-751, doi:10.1007/s00421-007-0552-2), and charges less; two
 * below, a clearly suppressed night, charges less still. A night above the
 * baseline stays neutral: a rise also comes with functional overreaching (Le
 * Meur et al., Med Sci Sports Exerc 2013;45(11):2061-2071,
 * doi:10.1249/MSS.0b013e3182980125), so it does not read as better recovery.
 * The multipliers and the second step are app heuristics, no published source.
 */
private fun hrvChargeMultiplier(
    baseline: HrvBaseline?,
    rmssdMs: Double,
): Double {
    val deviations = baseline?.standardDeviationsFromMean(rmssdMs) ?: return 1.0
    return when {
        deviations < -2.0 -> 0.75
        deviations < -1.0 -> 0.90
        else -> 1.0
    }
}

internal fun respiratoryStressFactor(
    baseline: Double?,
    average: Double?,
): RespiratoryFactor {
    if (baseline == null || average == null) return RespiratoryFactor(1.0, 1.0)
    return when {
        average >= baseline + 3.0 -> RespiratoryFactor(drainMultiplier = 1.12, chargePenalty = 1.15)
        average >= baseline + 1.5 -> RespiratoryFactor(drainMultiplier = 1.05, chargePenalty = 1.06)
        else -> RespiratoryFactor(1.0, 1.0)
    }
}
