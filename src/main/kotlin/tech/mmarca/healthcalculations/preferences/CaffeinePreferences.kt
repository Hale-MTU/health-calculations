package tech.mmarca.healthcalculations.preferences

import java.time.LocalTime
import kotlin.math.roundToInt

data class CaffeinePreferences(
    val profileCompleted: Boolean = false,
    val halfLifeMinutes: Int = DefaultHalfLifeMinutes,
    val absorptionMinutes: Int = DefaultAbsorptionMinutes,
    val sleepThresholdMg: Int = DefaultSleepThresholdMg,
    val bedtime: LocalTime = DefaultBedtime,
    val sleepSensitivity: CaffeineSleepSensitivity = CaffeineSleepSensitivity.NORMAL,
    val smoker: Boolean = false,
    val alcoholUse: CaffeineAlcoholUse = CaffeineAlcoholUse.NONE,
    val caffeineHabituation: CaffeineHabituation = CaffeineHabituation.MODERATE,
    val liverImpairment: Boolean = false,
    val medicationInteraction: Boolean = false,
    val cyp1a2Genotype: CaffeineGenotype = CaffeineGenotype.UNKNOWN,
    val ahrGenotype: CaffeineGenotype = CaffeineGenotype.UNKNOWN,
    val hormonalStatus: CaffeineHormonalStatus = CaffeineHormonalStatus.NONE,
) {
    fun normalized(): CaffeinePreferences =
        copy(
            halfLifeMinutes = halfLifeMinutes.coerceIn(MinHalfLifeMinutes, MaxHalfLifeMinutes),
            absorptionMinutes = absorptionMinutes.coerceIn(MinAbsorptionMinutes, MaxAbsorptionMinutes),
            sleepThresholdMg = sleepThresholdMg.coerceIn(MinSleepThresholdMg, MaxSleepThresholdMg),
        )

    /**
     * The half-life with this profile's factors applied, each a multiplier on [halfLifeMinutes].
     *
     * Smoking: 3.5 h in smokers against 6.0 h in non-smokers (Parsons and Neims, Clin Pharmacol
     * Ther 1978;24(1):40-45, doi:10.1002/cpt197824140), so 0.6. The oral contraceptive and
     * pregnancy factors are on [CaffeineHormonalStatus].
     *
     * Age and weight do not change it. Young and elderly men eliminate caffeine alike (Blanchard
     * and Sawers, J Pharmacokinet Biopharm 1983;11(2):109-126, doi:10.1007/BF01061844), and
     * obesity widens the volume of distribution without changing clearance (Abernethy, Todd and
     * Schwartz, Br J Clin Pharmacol 1985;20(1):61-66, PMID 4027137). [bodyProfile] is kept so
     * the apps' calls stay the same; it no longer moves the result.
     *
     * Liver impairment (1.8) and a medication interaction (1.4): app heuristic, a floor rather
     * than a typical value. Alcoholic liver disease has been measured at 60 and 168 h (Statland
     * and Demas, Am J Clin Pathol 1980;73(3):390-393, doi:10.1093/ajcp/73.3.390), and a strong
     * CYP1A2 inhibitor lengthens it many times over; the result is capped at
     * [MaxEffectiveHalfLifeMinutes].
     */
    fun effectiveHalfLifeMinutes(@Suppress("UNUSED_PARAMETER") bodyProfile: BodyProfile): Int {
        val base = halfLifeMinutes.toDouble()
        val multiplier = listOf(
            sleepSensitivity.halfLifeMultiplier,
            alcoholUse.halfLifeMultiplier,
            caffeineHabituation.halfLifeMultiplier,
            cyp1a2Genotype.halfLifeMultiplier,
            ahrGenotype.halfLifeMultiplier,
            hormonalStatus.halfLifeMultiplier,
            if (smoker) 0.6 else 1.0,
            if (liverImpairment) 1.8 else 1.0,
            if (medicationInteraction) 1.4 else 1.0,
        ).fold(1.0) { product, factor -> product * factor }
        return (base * multiplier)
            .roundToInt()
            .coerceIn(MinHalfLifeMinutes, MaxEffectiveHalfLifeMinutes)
    }

    companion object {
        /**
         * Five hours: healthy adults' half-life lies between about 3 and 7 h (Temple et al.,
         * "The safety of ingested caffeine: a comprehensive review", Front Psychiatry 2017;8:80,
         * doi:10.3389/fpsyt.2017.00080).
         */
        const val DefaultHalfLifeMinutes = 300
        const val DefaultAbsorptionMinutes = 45
        const val DefaultSleepThresholdMg = 60
        val DefaultBedtime: LocalTime = LocalTime.of(22, 30)
        const val DefaultConsumptionDurationMinutes = 10
        const val MinHalfLifeMinutes = 90
        const val MaxHalfLifeMinutes = 720
        const val MaxEffectiveHalfLifeMinutes = 1_080
        const val MinAbsorptionMinutes = 10
        const val MaxAbsorptionMinutes = 180
        const val MinSleepThresholdMg = 5
        const val MaxSleepThresholdMg = 300
    }
}

/**
 * App heuristic, no published source. Sensitivity to caffeine's effect on sleep is a matter of
 * the adenosine receptor, not of elimination; the multiplier stands in for a lower threshold.
 */
enum class CaffeineSleepSensitivity(val halfLifeMultiplier: Double) {
    LOW(0.9),
    NORMAL(1.0),
    HIGH(1.2),
    INSOMNIA(1.35),
}

/** App heuristic, no published source. */
enum class CaffeineAlcoholUse(val halfLifeMultiplier: Double) {
    NONE(1.0),
    OCCASIONAL(1.05),
    REGULAR(1.15),
}

/** App heuristic, no published source: tolerance blunts the effect more than it speeds elimination. */
enum class CaffeineHabituation(val halfLifeMultiplier: Double) {
    LOW(1.1),
    MODERATE(1.0),
    HIGH(0.95),
}

/** CYP1A2 or AHR metaboliser type. App heuristic, no published source for the multipliers. */
enum class CaffeineGenotype(val halfLifeMultiplier: Double) {
    UNKNOWN(1.0),
    FAST(0.85),
    NORMAL(1.0),
    SLOW(1.25),
}

/**
 * Oral contraceptives: 7.88 h against 5.37 h in matched controls on low-dose oestrogen pills,
 * clearance impaired and volume unchanged (Abernethy and Todd, Eur J Clin Pharmacol
 * 1985;28(4):425-428, doi:10.1007/BF00544361), so 1.5.
 *
 * Pregnancy: 8.3 h in pregnant women against 3.4 h outside pregnancy (Knutti, Rothweiler and
 * Schlatter, Eur J Clin Pharmacol 1981;21(2):121-126, doi:10.1007/BF00637512), so 2.4. It rises
 * through pregnancy, to 10.5 h in its last four weeks against 3 h (Knutti et al., Arch Toxicol
 * Suppl 1982;5:187-192, doi:10.1007/978-3-642-68511-8_33): late in pregnancy this understates it.
 */
enum class CaffeineHormonalStatus(val halfLifeMultiplier: Double) {
    NONE(1.0),
    ORAL_CONTRACEPTIVE(1.5),
    PREGNANT(2.4),
}
