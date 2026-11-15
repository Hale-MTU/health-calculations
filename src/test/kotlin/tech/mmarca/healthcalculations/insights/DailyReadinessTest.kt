package tech.mmarca.healthcalculations.insights

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.healthcalculations.model.DashboardWeeklyCardioLoad
import tech.mmarca.healthcalculations.model.DashboardWeeklyCardioLoadTargetSource
import tech.mmarca.healthcalculations.model.DashboardWeeklyIntensityMinutes
import tech.mmarca.healthcalculations.model.DaySignal
import tech.mmarca.healthcalculations.model.DaySignals

class DailyReadinessTest {


    @Test
    fun readyWhenSleepAndRecoverySignalsAreStrong() {
        val insight = calculateDailyReadiness(
            DaySignals(
                sleepScore = sleepScore(score = 88, hours = 8.0),
                restingHeartRateBpm = 55,
                restingHeartRateBaselineBpm = 58,
                hrvRmssdMs = 62.0,
                hrvBaselineRmssdMs = 56.0,
                avgHeartRateBpm = 68,
                weeklyCardioLoad = weeklyLoad(current = 90, target = 100, today = 12),
                weeklyIntensityMinutes = weeklyIntensity(minutes = 160, today = 34),
                mindfulnessMinutes = 10,
                loadedMetrics = setOf(
                    DaySignal.SLEEP,
                    DaySignal.AVG_HEART_RATE,
                    DaySignal.RESTING_HEART_RATE,
                    DaySignal.HRV,
                    DaySignal.INTENSITY_MINUTES,
                ),
            )
        )

        assertEquals(ReadinessState.READY, insight.state)
        assertEquals(ReadinessRecommendationType.HARD_TRAINING, insight.recommendationType)
        assertEquals(ReadinessConfidence.HIGH, insight.confidence)
        assertEquals(HrvStatus.BALANCED, insight.hrvStatus.status)
        assertEquals(IntensityMinutesStatus.GOAL_MET, insight.intensityMinutes.status)
        assertEquals(PhysiologicalStressLevel.RESTING, insight.physiologicalStress.level)
        assertTrue(insight.score >= 80)
        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.HRV_NORMAL })
        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.INTENSITY_MINUTES_ON_TARGET })
        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.RESTING_HR_NORMAL })
    }

    @Test
    fun recoveryDayWhenSleepHrvAndRestingHeartRateArePoor() {
        val insight = calculateDailyReadiness(
            DaySignals(
                sleepScore = sleepScore(score = 36, hours = 5.0),
                restingHeartRateBpm = 68,
                restingHeartRateBaselineBpm = 58,
                hrvRmssdMs = 35.0,
                hrvBaselineRmssdMs = 55.0,
                avgHeartRateBpm = 94,
                weeklyCardioLoad = weeklyLoad(current = 145, target = 100, today = 20),
                loadedMetrics = setOf(
                    DaySignal.SLEEP,
                    DaySignal.AVG_HEART_RATE,
                    DaySignal.RESTING_HEART_RATE,
                    DaySignal.HRV,
                ),
            )
        )

        assertEquals(ReadinessState.REST, insight.state)
        assertEquals(ReadinessRecommendationType.REST, insight.recommendationType)
        assertEquals(HrvStatus.UNUSUALLY_LOW, insight.hrvStatus.status)
        assertEquals(PhysiologicalStressLevel.HIGH, insight.physiologicalStress.level)
        assertTrue(insight.recoveryModeSuggested)
        assertTrue(insight.score < 40)
        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.STRESS_HIGH })
        assertTrue(insight.recommendation.contains("Avoid intense training"))
    }

    @Test
    fun hrvStatusUsesPersonalBaselineThresholds() {
        assertEquals(
            HrvStatus.BALANCED,
            calculateHrvStatus(hrvRmssdMs = 51.0, baselineRmssdMs = 50.0, hasHrvData = true).status,
        )
        assertEquals(
            HrvStatus.LOW,
            calculateHrvStatus(hrvRmssdMs = 42.0, baselineRmssdMs = 50.0, hasHrvData = true).status,
        )
        assertEquals(
            HrvStatus.UNUSUALLY_LOW,
            calculateHrvStatus(hrvRmssdMs = 34.0, baselineRmssdMs = 50.0, hasHrvData = true).status,
        )
        assertEquals(
            HrvStatus.HIGH,
            calculateHrvStatus(hrvRmssdMs = 58.0, baselineRmssdMs = 50.0, hasHrvData = true).status,
        )
        assertEquals(
            HrvStatus.UNUSUALLY_HIGH,
            calculateHrvStatus(hrvRmssdMs = 66.0, baselineRmssdMs = 50.0, hasHrvData = true).status,
        )
        assertEquals(
            HrvStatus.NEEDS_MORE_HRV,
            calculateHrvStatus(hrvRmssdMs = null, baselineRmssdMs = 50.0, hasHrvData = true).status,
        )
        assertEquals(
            HrvStatus.NEEDS_MORE_HRV,
            calculateHrvStatus(hrvRmssdMs = 50.0, baselineRmssdMs = null, hasHrvData = true).status,
        )
        assertEquals(
            HrvStatus.NEEDS_MORE_HRV,
            calculateHrvStatus(hrvRmssdMs = 50.0, baselineRmssdMs = 50.0, hasHrvData = false).status,
        )
    }

    @Test
    fun checkSymptomsWhenTemperatureSignalIsUnusual() {
        val insight = calculateDailyReadiness(
            DaySignals(
                sleepScore = sleepScore(score = 55, hours = 6.0),
                restingHeartRateBpm = 67,
                restingHeartRateBaselineBpm = 58,
                hrvRmssdMs = 38.0,
                hrvBaselineRmssdMs = 55.0,
                latestBodyTemperatureCelsius = 38.0,
                loadedMetrics = setOf(
                    DaySignal.SLEEP,
                    DaySignal.RESTING_HEART_RATE,
                    DaySignal.HRV,
                    DaySignal.BODY_TEMPERATURE,
                ),
            )
        )

        assertEquals(ReadinessState.REST, insight.state)
        assertEquals(ReadinessRecommendationType.CHECK_SYMPTOMS, insight.recommendationType)
        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.TEMPERATURE_ELEVATED })
        assertTrue(insight.recommendation.contains("If you feel unwell"))
    }

    @Test
    fun intensityMinutesReadinessUsesWeeklyPace() {
        assertEquals(
            IntensityMinutesStatus.GOAL_MET,
            calculateIntensityMinutesReadiness(
                weeklyIntensityMinutes = weeklyIntensity(minutes = 151, today = 20),
                hasIntensityData = true,
            ).status,
        )
        assertEquals(
            IntensityMinutesStatus.ON_TRACK,
            calculateIntensityMinutesReadiness(
                weeklyIntensityMinutes = weeklyIntensity(minutes = 70, today = 10, daysElapsed = 3),
                hasIntensityData = true,
            ).status,
        )
        assertEquals(
            IntensityMinutesStatus.BEHIND,
            calculateIntensityMinutesReadiness(
                weeklyIntensityMinutes = weeklyIntensity(minutes = 90, today = 0, daysElapsed = 6),
                hasIntensityData = true,
            ).status,
        )
        assertEquals(
            IntensityMinutesStatus.NEEDS_MORE_DATA,
            calculateIntensityMinutesReadiness(
                weeklyIntensityMinutes = null,
                hasIntensityData = true,
            ).status,
        )
    }

    @Test
    fun unknownWhenNoSignalsAreAvailable() {
        val insight = calculateDailyReadiness(DaySignals())

        assertEquals(ReadinessState.UNKNOWN, insight.state)
        assertEquals(ReadinessConfidence.LOW, insight.confidence)
        assertEquals(0, insight.score)
        assertTrue(insight.explanation.contains("not enough local data"))
    }

    @Test
    fun explanationJoinsFactorDetailsWithoutDoublePunctuation() {
        val insight = calculateDailyReadiness(
            DaySignals(
                sleepScore = sleepScore(score = 36, hours = 5.0),
                restingHeartRateBpm = 68,
                restingHeartRateBaselineBpm = 58,
                hrvRmssdMs = 35.0,
                hrvBaselineRmssdMs = 55.0,
                avgHeartRateBpm = 94,
                weeklyCardioLoad = weeklyLoad(current = 145, target = 100, today = 20),
                loadedMetrics = setOf(
                    DaySignal.SLEEP,
                    DaySignal.AVG_HEART_RATE,
                    DaySignal.RESTING_HEART_RATE,
                    DaySignal.HRV,
                ),
            ),
        )

        assertFalse(insight.explanation.contains("., and"))
        assertTrue(insight.explanation.endsWith("."))
    }

    @Test
    fun nutritionFactorNotShownWhenOnlyHydrationIsLogged() {
        val insight = calculateDailyReadiness(
            DaySignals(
                hydrationLiters = 1.5,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 0.0,
                loadedMetrics = setOf(
                    DaySignal.HYDRATION,
                ),
            ),
            goals = DailyReadinessGoalInputs(hydrationLitersGoal = 2.0),
        )

        assertFalse(insight.factors.any { it.kind == ReadinessFactorKind.NUTRITION_LOGGED })
    }

    @Test
    fun nutritionFactorShownWhenMealDataIsPresent() {
        val insight = calculateDailyReadiness(
            DaySignals(
                caloriesInKcal = 1_800.0,
                loadedMetrics = emptySet(),
            ),
        )

        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.NUTRITION_LOGGED })
    }

    @Test
    fun lowConfidenceWhenBaselinesAreMissing() {
        val insight = calculateDailyReadiness(
            DaySignals(
                sleepScore = sleepScore(score = 76, hours = 7.0),
                restingHeartRateBpm = 56,
                hrvRmssdMs = 50.0,
                loadedMetrics = setOf(
                    DaySignal.SLEEP,
                    DaySignal.RESTING_HEART_RATE,
                    DaySignal.HRV,
                ),
            )
        )

        assertEquals(ReadinessConfidence.LOW, insight.confidence)
        assertEquals("new_user_not_enough_baseline", insight.confidenceReason)
        assertEquals(HrvStatus.NEEDS_MORE_HRV, insight.hrvStatus.status)
        assertTrue(insight.factors.any { it.kind == ReadinessFactorKind.NEW_USER_NOT_ENOUGH_BASELINE })
    }


    // Measured body energy.

    @Test
    fun measuredBodyEnergyReplacesTheEstimate() {
        // The deltas estimate the quantity the body-energy engine measures. When the engine answers, it wins.
        val withoutTimeline = calculateDailyReadiness(baseData())
        val withTimeline = calculateDailyReadiness(baseData(bodyEnergy = timeline(current = 64, start = 71)))

        assertEquals(64, withTimeline.bodyEnergyScore)
        assertTrue(withoutTimeline.bodyEnergyScore != 64)
        assertTrue(withTimeline.factors.none { it.kind in BodyEnergyFactorKinds })
    }

    @Test
    fun noTimelineLeavesTheEstimatePathIntact() {
        val insight = calculateDailyReadiness(baseData())

        assertTrue(insight.bodyEnergyScore in 1..100)
        assertTrue(insight.factors.none { it.kind in BodyEnergyFactorKinds })
    }

    @Test
    fun drainedBodyEnergyHoldsTrainingBack() {
        val rested = calculateDailyReadiness(baseData())
        val drained = calculateDailyReadiness(baseData(bodyEnergy = timeline(current = 18, start = 44)))

        assertEquals(18, drained.bodyEnergyScore)
        assertEquals(rested.score - 20, drained.score)
        assertEquals(rested.trainingReadinessScore - 20, drained.trainingReadinessScore)
        val factor = drained.factors.single { it.kind == ReadinessFactorKind.BODY_ENERGY_DRAINED }
        assertEquals(ReadinessFactorImpact.WARNING, factor.impact)
        assertEquals(listOf(18, 44), factor.args)
        assertEquals("Body energy is at 18 after starting the day at 44.", factor.detail)
    }

    @Test
    fun lowBodyEnergyIsANegativeFactor() {
        val rested = calculateDailyReadiness(baseData())
        val low = calculateDailyReadiness(baseData(bodyEnergy = timeline(current = 40, start = 70)))

        assertEquals(rested.score - 8, low.score)
        assertEquals(rested.trainingReadinessScore - 9, low.trainingReadinessScore)
        val factor = low.factors.single { it.kind == ReadinessFactorKind.BODY_ENERGY_LOW }
        assertEquals(ReadinessFactorImpact.NEGATIVE, factor.impact)
        assertEquals(listOf(40, 70), factor.args)
    }

    @Test
    fun aDrainedStartCountsEvenWhenTheDayHasRecovered() {
        // Starting the day at 30 or below is its own signal.
        val low = calculateDailyReadiness(baseData(bodyEnergy = timeline(current = 62, start = 28)))

        assertTrue(low.factors.any { it.kind == ReadinessFactorKind.BODY_ENERGY_LOW })
    }

    @Test
    fun chargedBodyEnergyIsAPositiveFactor() {
        val rested = calculateDailyReadiness(baseData())
        val charged = calculateDailyReadiness(baseData(bodyEnergy = timeline(current = 88, start = 62)))

        assertEquals(rested.score + 6, charged.score)
        assertEquals(rested.trainingReadinessScore + 5, charged.trainingReadinessScore)
        val factor = charged.factors.single { it.kind == ReadinessFactorKind.BODY_ENERGY_CHARGED }
        assertEquals(ReadinessFactorImpact.POSITIVE, factor.impact)
        assertEquals(listOf(88, 62), factor.args)
    }

    @Test
    fun anOrdinaryBatteryAddsNoFactorAtAll() {
        val insight = calculateDailyReadiness(baseData(bodyEnergy = timeline(current = 62, start = 70)))

        assertEquals(62, insight.bodyEnergyScore)
        assertTrue(insight.factors.none { it.kind in BodyEnergyFactorKinds })
    }

    private fun baseData(bodyEnergy: BodyEnergyTimeline? = null): DaySignals =
        DaySignals(
            sleepScore = sleepScore(score = 82, hours = 8.0),
            restingHeartRateBpm = 55,
            restingHeartRateBaselineBpm = 56,
            hrvRmssdMs = 58.0,
            hrvBaselineRmssdMs = 56.0,
            avgHeartRateBpm = 66,
            bodyEnergyTimeline = bodyEnergy,
            loadedMetrics = setOf(
                DaySignal.SLEEP,
                DaySignal.AVG_HEART_RATE,
                DaySignal.RESTING_HEART_RATE,
                DaySignal.HRV,
            ),
        )

    private fun timeline(current: Int, start: Int): BodyEnergyTimeline =
        BodyEnergyTimeline(
            date = LocalDate.of(2026, 6, 10),
            startScore = start,
            currentScore = current,
            charged = 0,
            drained = 0,
            points = emptyList(),
            confidence = BodyEnergyConfidence.HIGH,
            confidenceReason = "test",
        )

    private fun sleepScore(
        score: Int,
        hours: Double,
    ): SleepScoreEstimate =
        SleepScoreEstimate(
            score = score,
            confidence = SleepScoreConfidence.HIGH,
            sleepDurationMinutes = hours * 60.0,
            timeInBedMinutes = hours * 60.0,
        )

    private fun weeklyLoad(
        current: Int,
        target: Int,
        today: Int,
    ): DashboardWeeklyCardioLoad =
        DashboardWeeklyCardioLoad(
            currentScore = current,
            targetScore = target,
            todayScore = today,
            confidence = CardioLoadConfidence.HIGH,
            targetSource = DashboardWeeklyCardioLoadTargetSource.RECENT_HISTORY,
        )

    private fun weeklyIntensity(
        minutes: Int,
        today: Int,
        daysElapsed: Int = 7,
        confidence: IntensityMinutesConfidence = IntensityMinutesConfidence.HIGH,
    ): DashboardWeeklyIntensityMinutes =
        DashboardWeeklyIntensityMinutes(
            moderateMinutes = minutes,
            vigorousMinutes = 0,
            moderateEquivalentMinutes = minutes,
            todayModerateEquivalentMinutes = today,
            daysElapsed = daysElapsed,
            confidence = confidence,
        )
}

private val BodyEnergyFactorKinds = setOf(
    ReadinessFactorKind.BODY_ENERGY_DRAINED,
    ReadinessFactorKind.BODY_ENERGY_LOW,
    ReadinessFactorKind.BODY_ENERGY_CHARGED,
)
