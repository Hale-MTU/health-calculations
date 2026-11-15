// One module, no dependency beyond the Kotlin standard library: an app that
// includes it takes no transitive code. Health Connect's exercise and segment
// types are mirrored as constants (model/ExerciseSessionRecord.kt); each app
// checks the mirror against the real classes in its own tests.
plugins {
    alias(libs.plugins.kotlin.jvm)
    // The Wear OS sleep-minute fixtures, for the tests here and for OpenWatch's
    // sync tests: testImplementation(testFixtures("tech.mmarca.healthcalculations:health-calculations")).
    `java-test-fixtures`
}

group = "tech.mmarca.healthcalculations"
version = providers.gradleProperty("healthcalculations.version").orElse("0.1.0-SNAPSHOT").get()

kotlin {
    jvmToolchain(17)
}

dependencies {
    testFixturesImplementation(libs.truth)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.org.json)
}

tasks.withType<Test>().configureEach {
    // The apps' tests run in English; a locale-sensitive assertion should not
    // depend on the machine.
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
}

// The whole gate in one invocation, as in the apps.
tasks.register("verifyCi") {
    group = "verification"
    description = "Runs the unit tests."
    dependsOn("test")
}
