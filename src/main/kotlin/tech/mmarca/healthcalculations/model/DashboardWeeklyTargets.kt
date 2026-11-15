package tech.mmarca.healthcalculations.model

import kotlin.math.roundToInt
import tech.mmarca.healthcalculations.insights.CardioLoadConfidence
import tech.mmarca.healthcalculations.insights.DefaultWeeklyIntensityMinutesTarget
import tech.mmarca.healthcalculations.insights.IntensityMinutesConfidence

data class DashboardWeeklyCardioLoad(
    val currentScore: Int,
    val targetScore: Int,
    val todayScore: Int,
    val confidence: CardioLoadConfidence,
    val targetSource: DashboardWeeklyCardioLoadTargetSource,
) {
    val progressFraction: Float
        get() = if (targetScore > 0) {
            (currentScore / targetScore.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val progressPercent: Int
        get() = (progressFraction * 100f).roundToInt()

    val todayProgressPercent: Int
        get() = if (targetScore > 0) {
            (todayScore * 100.0 / targetScore).roundToInt().coerceAtLeast(0)
        } else {
            0
        }
}

enum class DashboardWeeklyCardioLoadTargetSource {
    RECENT_HISTORY,
    CURRENT_PACE,
}

data class DashboardWeeklyIntensityMinutes(
    val moderateMinutes: Int,
    val vigorousMinutes: Int,
    val moderateEquivalentMinutes: Int,
    val targetMinutes: Int = DefaultWeeklyIntensityMinutesTarget,
    val todayModerateEquivalentMinutes: Int,
    val daysElapsed: Int,
    val confidence: IntensityMinutesConfidence,
) {
    val progressFraction: Float
        get() = if (targetMinutes > 0) {
            (moderateEquivalentMinutes / targetMinutes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val progressPercent: Int
        get() = (progressFraction * 100f).roundToInt()

    val expectedByNowMinutes: Int
        get() = if (targetMinutes > 0) {
            (targetMinutes * daysElapsed.coerceIn(1, 7) / 7.0).roundToInt().coerceAtLeast(1)
        } else {
            0
        }

    val isOnPace: Boolean
        get() = moderateEquivalentMinutes >= expectedByNowMinutes
}
