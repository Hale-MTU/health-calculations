package tech.mmarca.healthcalculations.model

import java.time.Instant
import tech.mmarca.healthcalculations.insights.BodyEnergyTimeline
import tech.mmarca.healthcalculations.insights.SleepScoreEstimate

/**
 * One day's figures that the daily readiness and the physiological stress
 * estimate score. An app fills it from its own day aggregate.
 *
 * A figure counts only when its [DaySignal] is in [loadedMetrics]: a signal
 * that was not read is unknown, not zero, and the scores leave it out rather
 * than read a default as a measurement. The fields without a signal
 * (nutrition, mindfulness, the Body Energy timeline, the weekly totals) are
 * null when they were not read.
 */
data class DaySignals(
    val loadedMetrics: Set<DaySignal> = emptySet(),
    val avgHeartRateBpm: Long = 0,
    val heartRateSampleCount: Int = 0,
    val heartRateSampleStartTime: Instant? = null,
    val heartRateSampleEndTime: Instant? = null,
    val restingHeartRateBpm: Long = 0,
    val restingHeartRateBaselineBpm: Long? = null,
    val hrvRmssdMs: Double? = null,
    val hrvBaselineRmssdMs: Double? = null,
    val hrvSampleCount: Int = 0,
    val hrvSampleStartTime: Instant? = null,
    val hrvSampleEndTime: Instant? = null,
    val sleepScore: SleepScoreEstimate = SleepScoreEstimate.NoData,
    val hydrationLiters: Double = 0.0,
    val mindfulnessMinutes: Int? = null,
    val caloriesInKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
    val latestBodyTemperatureCelsius: Double? = null,
    val latestSkinTemperatureDeltaCelsius: Double? = null,
    val weeklyCardioLoad: DashboardWeeklyCardioLoad? = null,
    val weeklyIntensityMinutes: DashboardWeeklyIntensityMinutes? = null,
    val workouts: List<ExerciseData> = emptyList(),
    val bodyEnergyTimeline: BodyEnergyTimeline? = null,
)

/** The signals of [DaySignals] whose presence is recorded, because their unread value looks like a reading. */
enum class DaySignal {
    SLEEP,
    HRV,
    RESTING_HEART_RATE,
    AVG_HEART_RATE,
    INTENSITY_MINUTES,
    BODY_TEMPERATURE,
    SKIN_TEMPERATURE,
    HYDRATION,
}
