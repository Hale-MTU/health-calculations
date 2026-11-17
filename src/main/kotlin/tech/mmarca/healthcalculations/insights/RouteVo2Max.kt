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

/** A stretch longer than this spans a gap in the route, not a steady minute. App heuristic, no published source. */
private val MaxVo2MaxStretch: Duration = Duration.ofMinutes(2)

/**
 * The distance of a stretch adds up fixes at least this far apart. GPS error
 * makes a track longer than the way, the more so the denser its fixes (Ranacher
 * et al., "Why GPS makes distances bigger than they are", Int J Geogr Inf Sci
 * 2016;30(2):316-333, doi:10.1080/13658816.2015.1086924). The spacing itself is
 * an app heuristic, no published source.
 */
private val Vo2MaxFixSpacing: Duration = Duration.ofSeconds(10)

/** The sports the ACSM's walking and running equations on the level hold for, outdoors. */
private val Vo2MaxExerciseTypes = setOf(
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
)

/**
 * VO2max, ml/kg/min, from a walk or run outdoors. For each steady stretch of
 * one to two minutes of [route] inside one of [activeWindows], the net oxygen
 * cost of its speed by the ACSM's metabolic equations on the level
 * ([AcsmDistanceOnFoot]: 0.1 ml/kg/min per m/min walking, 0.2 running), divided
 * by the fraction of heart rate reserve, plus the resting 3.5 ml/kg/min: the
 * fraction of heart rate reserve tracks the fraction of VO2 reserve (Swain and
 * Leutholtz, Med Sci Sports Exerc 1997;29(3):410-414,
 * doi:10.1097/00005768-199703000-00018). The heart rate of a stretch is
 * [timeWeightedMeanHeartRate] of its readings; the estimate is the median of
 * the stretches.
 *
 * Flat ground is assumed: the grade terms need an elevation GPS gives too
 * coarsely. That is why hiking, mostly up or down hill, gives no estimate. A
 * stretch counts only at a pace the ACSM's table gives an equation for:
 * walking at 50 to 100 m/min, running above 134 m/min, in a run or a walk that
 * fast. Stretches outside 40 to 90 % of the reserve are left out, and fewer
 * than five give no estimate (app heuristics). The watches that publish such
 * estimates restrict them to walking and running the same way.
 *
 * Null for another sport, without a reserve, or with too few stretches.
 */
fun estimateRouteVo2Max(
    exerciseType: Int,
    route: List<ExerciseRoutePoint>,
    samples: List<HeartRateSample>,
    activeWindows: List<CardioLoadTimeWindow>,
    restingHeartRateBpm: Long,
    maxHeartRateBpm: Long,
): Double? {
    if (exerciseType !in Vo2MaxExerciseTypes || maxHeartRateBpm <= restingHeartRateBpm) return null
    val points = route.sortedBy { it.time }
    val estimates = ArrayList<Double>()
    var from = 0
    for (to in 1 until points.size) {
        val stretch = points.subList(from, to + 1)
        val length = Duration.between(stretch.first().time, stretch.last().time)
        if (length < Vo2MaxStretch) continue
        from = to
        if (length > MaxVo2MaxStretch) continue
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

/** One stretch's estimate; null at a pace without an equation, without readings, or outside [Vo2MaxReserveRange]. */
private fun stretchVo2Max(
    exerciseType: Int,
    stretch: List<ExerciseRoutePoint>,
    samples: List<HeartRateSample>,
    restingHeartRateBpm: Long,
    maxHeartRateBpm: Long,
): Double? {
    val start = stretch.first().time
    val end = stretch.last().time
    val speed = spacedMetres(stretch) / (Duration.between(start, end).toMillis() / 60_000.0)
    val running = exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING ||
        speed > AcsmDistanceOnFoot.MinRunningPaceMetresPerMinute
    val mlPerKgMetre = AcsmDistanceOnFoot.netMlPerKgMetre(speed, running) ?: return null
    val readings = samples.filter { !it.time.isBefore(start) && !it.time.isAfter(end) }.map { it.time to it.beatsPerMinute }
    val bpm = timeWeightedMeanHeartRate(readings) ?: return null
    val reserve = (bpm - restingHeartRateBpm) / (maxHeartRateBpm - restingHeartRateBpm)
    if (reserve !in Vo2MaxReserveRange) return null
    return mlPerKgMetre * speed / reserve + AcsmDistanceOnFoot.RestingMlPerKgMinute
}

/** The stretch's distance over fixes at least [Vo2MaxFixSpacing] apart, its first and last fix always among them. */
private fun spacedMetres(stretch: List<ExerciseRoutePoint>): Double {
    val kept = arrayListOf(stretch.first())
    for (point in stretch.drop(1)) {
        if (Duration.between(kept.last().time, point.time) >= Vo2MaxFixSpacing) kept += point
    }
    if (kept.last() !== stretch.last()) kept += stretch.last()
    return kept.zipWithNext().sumOf { (a, b) -> haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude) }
}
