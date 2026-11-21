package tech.mmarca.healthcalculations.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaffeinePreferencesTest {

    private val base = CaffeinePreferences(halfLifeMinutes = 300, sleepThresholdMg = 60)

    @Test
    fun `smoking shortens the half-life to 0_7 of it`() {
        assertEquals(210, base.copy(smoker = true).effectiveHalfLifeMinutes())
    }

    @Test
    fun `oral contraceptives lengthen it by half and pregnancy to 2_4 times`() {
        val pill = base.copy(hormonalStatus = CaffeineHormonalStatus.ORAL_CONTRACEPTIVE)
        val pregnant = base.copy(hormonalStatus = CaffeineHormonalStatus.PREGNANT)
        assertEquals(450, pill.effectiveHalfLifeMinutes())
        assertEquals(720, pregnant.effectiveHalfLifeMinutes())
    }

    @Test
    fun `factors multiply`() {
        val smokingOnThePill = base.copy(smoker = true, hormonalStatus = CaffeineHormonalStatus.ORAL_CONTRACEPTIVE)
        assertEquals(315, smokingOnThePill.effectiveHalfLifeMinutes())
    }

    @Test
    fun `the result is capped at the longest effective half-life, and says so`() {
        // 300 x 2.4 x 1.8 = 1296 minutes.
        val capped = base.copy(hormonalStatus = CaffeineHormonalStatus.PREGNANT, liverImpairment = true)
        assertEquals(CaffeinePreferences.MaxEffectiveHalfLifeMinutes, capped.effectiveHalfLifeMinutes())
        assertTrue(capped.isEffectiveHalfLifeCapped())
        assertFalse(base.isEffectiveHalfLifeCapped())
    }

    @Test
    fun `sleep sensitivity and habituation move the threshold, not the half-life`() {
        val sensitive = base.copy(
            sleepSensitivity = CaffeineSleepSensitivity.INSOMNIA,
            caffeineHabituation = CaffeineHabituation.LOW,
        )
        assertEquals(300, sensitive.effectiveHalfLifeMinutes())
        // 60 x 0.74 x 0.91 = 40.4 mg.
        assertEquals(40, sensitive.effectiveSleepThresholdMg())
        assertEquals(60, base.effectiveSleepThresholdMg())
    }

    @Test
    fun `the threshold stays within its bounds`() {
        val low = base.copy(sleepThresholdMg = CaffeinePreferences.MinSleepThresholdMg, sleepSensitivity = CaffeineSleepSensitivity.INSOMNIA)
        assertEquals(CaffeinePreferences.MinSleepThresholdMg, low.effectiveSleepThresholdMg())
    }
}
