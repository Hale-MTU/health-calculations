package tech.mmarca.healthcalculations.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.LocalDate

data class ExerciseData(
    val id: String,
    val title: String?,
    val exerciseType: Int,
    val startTime: Instant,
    val endTime: Instant,
    val durationMs: Long,
    val source: String,
    val totalDistanceMeters: Double? = null,
    val totalCaloriesKcal: Double? = null,
    val activeCaloriesKcal: Double? = null,
    val steps: Long? = null,
    val wheelchairPushes: Long? = null,
    val averageSpeedMetersPerSecond: Double? = null,
    val averagePowerWatts: Double? = null,
    val averageStepsCadenceRate: Double? = null,
    val averageCyclingCadenceRpm: Double? = null,
    val averageHeartRateBpm: Long? = null,
    val floorsClimbed: Int? = null,
    val elevationGainedMeters: Double? = null,
    val notes: String? = null,
    val startZoneOffset: ZoneOffset? = null,
    val endZoneOffset: ZoneOffset? = null,
    val lastModifiedTime: Instant? = null,
    val clientRecordId: String? = null,
    val clientRecordVersion: Long? = null,
    val recordingMethod: Int? = null,
    val device: ExerciseDeviceData? = null,
    val plannedExerciseSessionId: String? = null,
    val segments: List<ExerciseSegmentData> = emptyList(),
    val laps: List<ExerciseLapData> = emptyList(),
    val route: ExerciseRouteData = ExerciseRouteData(),
    val isOwnEntry: Boolean = false,
    val totalCaloriesSource: CaloriesBurnedSource = if (totalCaloriesKcal != null) {
        CaloriesBurnedSource.RECORDED_TOTAL
    } else {
        CaloriesBurnedSource.NO_DATA
    },
) {
    val durationMinutes: Long get() = durationMs / 60_000
}

data class ExerciseDeviceData(
    val type: Int,
    val manufacturer: String?,
    val model: String?,
)

data class ExerciseSegmentData(
    val startTime: Instant,
    val endTime: Instant,
    val segmentType: Int,
    val repetitions: Int,
    val setIndex: Int? = null,
    /** Load lifted in this set, when recorded. */
    val weightKg: Double? = null,
) {
    val durationMs: Long get() = endTime.toEpochMilli() - startTime.toEpochMilli()
}

data class ExerciseLapData(
    val startTime: Instant,
    val endTime: Instant,
    val lengthMeters: Double?,
) {
    val durationMs: Long get() = endTime.toEpochMilli() - startTime.toEpochMilli()
}

data class ActivityRecordingLap(
    val startTime: Instant,
    val endTime: Instant,
    val distanceMeters: Double?,
) {
    val durationMs: Long get() = endTime.toEpochMilli() - startTime.toEpochMilli()
}

data class ExerciseRouteData(
    val status: ExerciseRouteStatus = ExerciseRouteStatus.NO_DATA,
    val points: List<ExerciseRoutePoint> = emptyList(),
)

enum class ExerciseRouteStatus {
    DATA,
    CONSENT_REQUIRED,
    NO_DATA,
}

data class ExerciseRoutePoint(
    val time: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val horizontalAccuracyMeters: Double?,
    val verticalAccuracyMeters: Double?,
)

data class ActivityPauseInterval(
    val startTime: Instant,
    val endTime: Instant,
)

data class DailySteps(
    val date: LocalDate,
    val steps: Long,
    val distanceMeters: Double,
    val wheelchairPushes: Long? = null,
    val floorsClimbed: Int? = null,
    val activeCaloriesKcal: Double? = null,
    val elevationGainedMeters: Double? = null,
)

/**
 * The intraday series bucket. It is the Body Energy timeline's bucket, so
 * steps and calories per bucket come straight from the read. A day is 288.
 */
val ActivityIntradayBucketDuration: Duration = Duration.ofMinutes(5)

data class ActivityProgressPoint(
    val time: Instant,
    val totalSteps: Long,
    val totalDistanceMeters: Double?,
    val totalCaloriesBurnedKcal: Double?,
    val totalActiveCaloriesKcal: Double? = null,
    val totalWheelchairPushes: Long? = null,
    val totalFloorsClimbed: Int? = null,
    val totalElevationGainedMeters: Double? = null,
)

data class SpeedSample(
    val time: Instant,
    val metersPerSecond: Double,
    val source: String,
)

enum class ActivityCadenceKind {
    CYCLING,
    STEPS,
}

data class ActivityCadenceSample(
    val time: Instant,
    val rate: Double,
    val kind: ActivityCadenceKind,
    val source: String,
)
