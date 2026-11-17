package tech.mmarca.healthcalculations.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class CaffeinePreferencesTest {

    private val base = CaffeinePreferences(halfLifeMinutes = 300)

    @Test
    fun `smoking shortens the half-life to 0_7 of it`() {
        assertEquals(210, base.copy(smoker = true).effectiveHalfLifeMinutes(BodyProfile()))
    }

    @Test
    fun `oral contraceptives lengthen it by half and pregnancy to 2_4 times`() {
        val pill = base.copy(hormonalStatus = CaffeineHormonalStatus.ORAL_CONTRACEPTIVE)
        val pregnant = base.copy(hormonalStatus = CaffeineHormonalStatus.PREGNANT)
        assertEquals(450, pill.effectiveHalfLifeMinutes(BodyProfile()))
        assertEquals(720, pregnant.effectiveHalfLifeMinutes(BodyProfile()))
    }

    @Test
    fun `factors multiply`() {
        val smokingOnThePill = base.copy(smoker = true, hormonalStatus = CaffeineHormonalStatus.ORAL_CONTRACEPTIVE)
        assertEquals(315, smokingOnThePill.effectiveHalfLifeMinutes(BodyProfile()))
    }

    @Test
    fun `the result is capped at the longest effective half-life`() {
        // 300 x 2.4 x 1.8 = 1296 minutes.
        val capped = base.copy(hormonalStatus = CaffeineHormonalStatus.PREGNANT, liverImpairment = true)
        assertEquals(CaffeinePreferences.MaxEffectiveHalfLifeMinutes, capped.effectiveHalfLifeMinutes(BodyProfile()))
    }

    @Test
    fun `age and weight do not move it`() {
        val older = BodyProfile(birthYear = 1940, weightKg = 120.0)
        assertEquals(300, base.effectiveHalfLifeMinutes(older))
    }
}
