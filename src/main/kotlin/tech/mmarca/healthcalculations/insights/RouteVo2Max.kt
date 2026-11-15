package tech.mmarca.healthcalculations.insights

import java.time.Duration
import tech.mmarca.healthcalculations.geo.haversineMeters
import tech.mmarca.healthcalculations.model.ExerciseRoutePoint
import tech.mmarca.healthcalculations.model.ExerciseSessionRecord
import tech.mmarca.healthcalculations.model.HeartRateSample
import tech.mmarca.healthcalculations.stats.medianOrNull

/** Only the minutes between this and this share of the heart rate reserve count. App heuristic, no published source. */
private val Vo2MaxReserveRange = 0.4..0.9

/** The fewest steady minutes an estimate rests on. App heuristic, no published source. */
private const val MinimumVo2MaxMinutes = 5

private val Vo2MaxStretch: Duration = Duration.ofMinutes(1)

/** The sports the ACSM's walking and running equations hold for, outdoors. */
private val Vo2MaxExerciseTypes = setOf(
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
)

/**
 * VO2max, ml/kg/min, from a walk, run or hike outdoors. For each steady
 * stretch of at least a minute of [route] inside one of [activeWindows], the
 * oxygen cost of its speed by the ACSM's metabolic equations on the level
 * ([AcsmDistanceOnFoot]: 3.5 + 0.1 ml/kg/min per m/min walking, 0.2 running),
 * divided by the fraction of heart rate reserve, because the fraction of heart
 * rate reserve tracks the fraction of VO2 reserve (Swain and Leutholtz, Med Sci
 * Sports Exerc 1997;29(3):410-414, doi:10.1097/00005768-199703000-00018). The
 * heart rate of a stretch is [timeWeightedMeanHeartRate] of its readings; the
 * estimate is the median of the stretches.
 *
 * Flat ground is assumed: the grade terms need an elevation GPS gives too
 * coarsely. A stretch counts at walking pace or faster, and running from the
 * ACSM's running pace or in a run. Stretches outside 40 to 90 % of the reserve
 * are left out, and fewer than five give no estimate (app heuristics). Only
 * walking, running and hiking: the equations are for those, and the watches
 * that publish such estimates restrict them the same way.
 *
 * Null for another sport, without a reserve, or with too few stretches.
 */
fun estimateRouteVo2Max(
    exerciseType: Int,
    route: List<ExerciseRoutePoint>,
    samples: List<HeartRateSample>,
    activeWindows: List<CardioLoadTimeWindow>,
    restingHeartRateBpm: Int,
    maxHeartRateBpm: Int,
): Double? {
    if (exerciseType !in Vo2MaxExerciseTypes || maxHeartRateBpm <= restingHeartRateBpm) return null
    val points = route.sortedBy { it.time }
    val estimates = ArrayList<Double>()
    var from = 0
    for (to in 1 until points.size) {
        val stretch = points.subList(from, to + 1)
        if (Duration.between(stretch.first().time, stretch.last().time) < Vo2MaxStretch) continue
        from = to
        val inside = activeWindows.any { window ->
            !window.start.isAfter(stretch.first().time) && !window.end.isBefore(stretch.last().time)
        }
        if (inside) {
            stretchVo2Max(exerciseType, stretch, samples, restingHeartRateBpm, maxHeartRateBpm)?.let(estimates::add)
        }
    }
    if (estimates.size < MinimumVo2MaxMinutes) return null
    return estimates.medianOrNull()
}

/** One stretch's estimate; null when it is too slow, has no readings, or sits outside [Vo2MaxReserveRange]. */
private fun stretchVo2Max(
    exerciseType: Int,
    stretch: List<ExerciseRoutePoint>,
    samples: List<HeartRateSample>,
    restingHeartRateBpm: Int,
    maxHeartRateBpm: Int,
): Double? {
    val start = stretch.first().time
    val end = stretch.last().time
    val metres = stretch.zipWithNext().sumOf { (a, b) -> haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude) }
    val speed = metres / (Duration.between(start, end).toMillis() / 60_000.0)
    if (speed < AcsmDistanceOnFoot.WalkingPaceMetresPerMinute.start) return null
    val readings = samples.filter { !it.time.isBefore(start) && !it.time.isAfter(end) }.map { it.time to it.beatsPerMinute }
    val bpm = timeWeightedMeanHeartRate(readings) ?: return null
    val reserve = (bpm - restingHeartRateBpm) / (maxHeartRateBpm - restingHeartRateBpm)
    if (reserve !in Vo2MaxReserveRange) return null
    val running = exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING ||
        speed > AcsmDistanceOnFoot.MinRunningPaceMetresPerMinute
    val mlPerKgMetre = if (running) AcsmDistanceOnFoot.RunningMlPerKgMetre else AcsmDistanceOnFoot.WalkingMlPerKgMetre
    val rest = AcsmDistanceOnFoot.RestingMlPerKgMinute
    return mlPerKgMetre * speed / reserve + rest
}
