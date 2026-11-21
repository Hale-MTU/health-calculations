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
     * Smoking: 3.0 h in smokers against 4.3 h in non-smokers, smoking the one covariate of
     * clearance in a population pharmacokinetic model (Seng et al., J Clin Pharm Ther
     * 2009;34(1):103-114, doi:10.1111/j.1365-2710.2008.00976.x), so 0.7. Older studies found a
     * larger effect, 3.5 h against 6.0 h (Parsons and Neims, Clin Pharmacol Ther 1978;24(1):40-45,
     * doi:10.1002/cpt197824140). The oral contraceptive and pregnancy factors are on
     * [CaffeineHormonalStatus].
     *
     * Weight: no factor. Clearance is unchanged in obesity: CYP1A2, which clears caffeine, was as
     * active in severe obesity as in controls (Kvitne et al., Br J Clin Pharmacol
     * 2022;88(9):4121-4133, doi:10.1111/bcp.15349) and in obese as in lean adolescents (Gade et
     * al., J Clin Pharmacol 2026;66(6):e70219, doi:10.1002/jcph.70219). The volume of
     * distribution grows with weight, so the half-life tends to be longer, 7.05 h against 5.40 h,
     * but not significantly (Abernethy, Todd and Schwartz, Br J Clin Pharmacol 1985;20(1):61-66,
     * PMID 4027137). No study gives a size to apply; leaving it at 1.0 is an app choice.
     *
     * Age: no factor. Young and elderly men eliminate caffeine alike (Blanchard and Sawers, J
     * Pharmacokinet Biopharm 1983;11(2):109-126, doi:10.1007/BF01061844), and no later study
     * found an age effect in adults. For under-18s no source was found either way: treating them
     * as adults is an app choice.
     *
     * Liver impairment (1.8) and a medication interaction (1.4): app heuristic, a floor rather
     * than a typical value. Alcoholic liver disease has been measured at 60 and 168 h (Statland
     * and Demas, Am J Clin Pathol 1980;73(3):390-393, doi:10.1093/ajcp/73.3.390), and a strong
     * CYP1A2 inhibitor lengthens it many times over; the result is capped at
     * [MaxEffectiveHalfLifeMinutes] ([isEffectiveHalfLifeCapped]).
     *
     * Sleep sensitivity and habituation change how much caffeine disturbs sleep, not how fast it
     * leaves: they move [effectiveSleepThresholdMg], not this.
     */
    fun effectiveHalfLifeMinutes(): Int =
        uncappedHalfLifeMinutes().roundToInt().coerceIn(MinHalfLifeMinutes, MaxEffectiveHalfLifeMinutes)

    /**
     * Whether the factors would take the half-life past [MaxEffectiveHalfLifeMinutes] or under
     * [MinHalfLifeMinutes], so [effectiveHalfLifeMinutes] shows the bound and a further factor
     * changes nothing.
     */
    fun isEffectiveHalfLifeCapped(): Boolean =
        uncappedHalfLifeMinutes().roundToInt() !in MinHalfLifeMinutes..MaxEffectiveHalfLifeMinutes

    private fun uncappedHalfLifeMinutes(): Double =
        halfLifeMinutes * listOf(
            alcoholUse.halfLifeMultiplier,
            cyp1a2Genotype.halfLifeMultiplier,
            ahrGenotype.halfLifeMultiplier,
            hormonalStatus.halfLifeMultiplier,
            if (smoker) 0.7 else 1.0,
            if (liverImpairment) 1.8 else 1.0,
            if (medicationInteraction) 1.4 else 1.0,
        ).fold(1.0) { product, factor -> product * factor }

    /**
     * The milligrams at bedtime above which caffeine counts against sleep: [sleepThresholdMg]
     * scaled by [sleepSensitivity] and [caffeineHabituation], within [MinSleepThresholdMg] and
     * [MaxSleepThresholdMg]. The same dose disturbs sleep more in people sensitive to it, a trait
     * tied to the adenosine A2A receptor gene, ADORA2A (Rétey et al., Clin Pharmacol Ther
     * 2007;81(5):692-698, doi:10.1038/sj.clpt.6100102), and tolerance blunts the effect without
     * speeding elimination. The sizes of both factors: app heuristic, no published source.
     */
    fun effectiveSleepThresholdMg(): Int =
        (sleepThresholdMg * sleepSensitivity.thresholdFactor * caffeineHabituation.thresholdFactor)
            .roundToInt()
            .coerceIn(MinSleepThresholdMg, MaxSleepThresholdMg)

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
 * How strongly caffeine disturbs this person's sleep: a factor on the sleep threshold
 * ([CaffeinePreferences.effectiveSleepThresholdMg]). The values are the inverse of the
 * half-life multipliers they replace (0.9, 1.2, 1.35): app heuristic, no published source.
 */
enum class CaffeineSleepSensitivity(val thresholdFactor: Double) {
    LOW(1.1),
    NORMAL(1.0),
    HIGH(0.83),
    INSOMNIA(0.74),
}

/** App heuristic, no published source. */
enum class CaffeineAlcoholUse(val halfLifeMultiplier: Double) {
    NONE(1.0),
    OCCASIONAL(1.05),
    REGULAR(1.15),
}

/**
 * Tolerance from habitual use: a factor on the sleep threshold
 * ([CaffeinePreferences.effectiveSleepThresholdMg]), since tolerance blunts the effect rather
 * than speeding elimination. The values are the inverse of the half-life multipliers they
 * replace (1.1, 0.95): app heuristic, no published source.
 */
enum class CaffeineHabituation(val thresholdFactor: Double) {
    LOW(0.91),
    MODERATE(1.0),
    HIGH(1.05),
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
 * 1985;28(4):425-428, doi:10.1007/BF00544361), so 1.5. A systematic analysis of 141 studies
 * confirms the lower clearance with oral contraceptives without pooling a size (Grzegorzewski et
 * al., Front Pharmacol 2022;12:752826, doi:10.3389/fphar.2021.752826); no later study measured it.
 *
 * Pregnancy: 8.3 h in pregnant women against 3.4 h outside pregnancy (Knutti, Rothweiler and
 * Schlatter, Eur J Clin Pharmacol 1981;21(2):121-126, doi:10.1007/BF00637512), so 2.4. A
 * pregnancy PBPK model calibrated on such data predicts caffeine exposure doubled by the third
 * trimester, through lower CYP1A2 activity (Gaohua et al., Br J Clin Pharmacol
 * 2012;74(5):873-885, doi:10.1111/j.1365-2125.2012.04363.x). It rises through pregnancy, to
 * 10.5 h in its last four weeks against 3 h (Knutti et al., Arch Toxicol Suppl 1982;5:187-192,
 * doi:10.1007/978-3-642-68511-8_33): late in pregnancy the ratio, 3.5, is larger than 2.4.
 * The factors are ratios and apply to the user's own [CaffeinePreferences.halfLifeMinutes], not
 * to the studies' absolute hours.
 */
enum class CaffeineHormonalStatus(val halfLifeMultiplier: Double) {
    NONE(1.0),
    ORAL_CONTRACEPTIVE(1.5),
    PREGNANT(2.4),
}
