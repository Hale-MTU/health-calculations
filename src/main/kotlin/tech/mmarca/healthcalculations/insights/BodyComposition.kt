package tech.mmarca.healthcalculations.insights

/*
 * Indices worked out from weight, height and body fat. One copy, so the
 * dashboard, the body screen and anything an app shares give the same number.
 */

/** Body mass index: weight in kg over height in metres squared (WHO). Null without a weight or a positive height. */
fun bodyMassIndex(weightKg: Double?, heightCm: Double?): Double? {
    val weight = weightKg ?: return null
    val meters = heightCm?.takeIf { it > 0.0 }?.let { it / 100.0 } ?: return null
    return weight / (meters * meters)
}

/**
 * Fat-free mass index: fat-free mass in kg over height in metres squared
 * (VanItallie et al. 1990, doi:10.1093/ajcn/52.6.953). Null without a positive
 * weight and height, or with a body fat outside 0 to under 100 %: a body that
 * is all fat is not a measurement.
 */
fun fatFreeMassIndex(weightKg: Double?, heightCm: Double?, bodyFatPercent: Double?): Double? {
    val weight = weightKg?.takeIf { it > 0.0 } ?: return null
    val meters = heightCm?.takeIf { it > 0.0 }?.let { it / 100.0 } ?: return null
    val fatRatio = bodyFatPercent?.takeIf { it >= 0.0 && it < 100.0 }?.let { it / 100.0 } ?: return null
    return weight * (1.0 - fatRatio) / (meters * meters)
}

/**
 * [fatFreeMassIndex] normalised to a height of 1.80 m by adding
 * 6.3 × (1.80 − height in m) (Kouri et al. 1995,
 * doi:10.1097/00042752-199510000-00003). Null when the index is.
 */
fun adjustedFatFreeMassIndex(weightKg: Double?, heightCm: Double?, bodyFatPercent: Double?): Double? {
    val index = fatFreeMassIndex(weightKg, heightCm, bodyFatPercent) ?: return null
    return index + 6.3 * (1.8 - checkNotNull(heightCm) / 100.0)
}
