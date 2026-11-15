package tech.mmarca.healthcalculations.stats

/**
 * The empty-list contract for aggregates. [averageOrNull] means "unknown";
 * [averageOrZero] means "the true total was zero". Never NaN, which slips
 * past every guard and renders literally.
 */
fun Iterable<Double>.averageOrNull(): Double? {
    var sum = 0.0
    var count = 0
    for (value in this) {
        sum += value
        count++
    }
    return if (count == 0) null else sum / count
}

@JvmName("averageOrNullOfLong")
fun Iterable<Long>.averageOrNull(): Double? {
    var sum = 0.0
    var count = 0
    for (value in this) {
        sum += value
        count++
    }
    return if (count == 0) null else sum / count
}

@JvmName("averageOrNullOfInt")
fun Iterable<Int>.averageOrNull(): Double? {
    var sum = 0.0
    var count = 0
    for (value in this) {
        sum += value
        count++
    }
    return if (count == 0) null else sum / count
}

fun Iterable<Double>.averageOrZero(): Double = averageOrNull() ?: 0.0

/**
 * The middle value; the mean of the two middle values for an even count. No values have no
 * median: an empty list throws, so a caller decides what "unknown" means.
 */
fun median(values: List<Double>): Double {
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
}

@JvmName("averageOrZeroOfLong")
fun Iterable<Long>.averageOrZero(): Double = averageOrNull() ?: 0.0
