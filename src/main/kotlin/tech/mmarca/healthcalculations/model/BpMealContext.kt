package tech.mmarca.healthcalculations.model

/**
 * When a blood-pressure reading was taken relative to meals. Health Connect
 * has no field for it, so an app that records it encodes [token] in its own
 * `clientRecordId`; foreign records fall back to time-of-day inference.
 */
enum class BpMealContext(val token: String) {
    BEFORE_BREAKFAST("beforebreakfast"),
    AFTER_BREAKFAST("afterbreakfast"),
    BEFORE_LUNCH("beforelunch"),
    AFTER_LUNCH("afterlunch"),
    BEFORE_DINNER("beforedinner"),
    AFTER_DINNER("afterdinner"),
}
