# health-calculations

The health calculations [OpenVitals](https://github.com/Hale-MTU/OpenVitals) and
[OpenWatch](https://github.com/Hale-MTU/OpenWatch) share: energy, heart rate, training load,
recovery, sleep, readiness and stress, the menstrual cycle, caffeine, and the watch's estimates
from its raw sensors. One copy, so a formula gives the same number in both apps.

Plain Kotlin on the JDK, with no dependency beyond the Kotlin standard library: no Android, no
Health Connect, no network. The phone apps, OpenWatch's `:wearlink` and the Wear OS app all
take the same code, and every calculation is tested on the JVM.

## What is in it

| Package | What |
| --- | --- |
| `insights` | The calculations: workout and walking energy (Keytel, Compendium METs, ACSM), basal metabolic rate (Mifflin-St Jeor), maximum heart rate, cardio load (TRIMP), intensity minutes, heart rate recovery, splits and route elevation, bioimpedance body composition, sleep score, Body Energy, daily readiness, physiological stress, caffeine, baselines and comparisons, and the watch's sleep, nap and resting heart rate estimators |
| `signal` | The Wear OS watch's raw sensors: beats from the PPG, RMSSD, respiratory rate, the accelerometer's per-minute features |
| `cycle` | Cycle estimates, phases, ovulation markers, temperature shifts, nightly vitals |
| `model` | The inputs and results the calculations read and return: exercise sessions, heart rate and HRV samples, sleep sessions and stages, vitals, cycle records, the day's signals for readiness |
| `preferences` | The settings a calculation reads: the body profile, the sleep window, the Body Energy calibration |
| `stats` | Averages and the median that never return NaN |
| `geo` | The haversine distance |

Each formula says where it comes from in its KDoc: the paper, the guideline or the
standard. A threshold without a published source says so ("app heuristic").

### Health Connect's constants

Calculations that depend on the kind of exercise read Health Connect's integer types.
`model/ExerciseSessionRecord.kt` and `model/ExerciseSegment.kt` copy them under Health
Connect's own names, every one of them, so a calculation reads
`ExerciseSessionRecord.EXERCISE_TYPE_RUNNING` and an app passes a record's `exerciseType`
straight in. Each app's `ExerciseTypeMirrorTest` checks the copy against the real classes,
so a Health Connect update that moves a value fails there.

### What stays in the apps

Reading and writing Health Connect, the screens and their formatting, the period navigation,
the drink catalogs (the caffeine calculations take a `CaffeineCatalog` the app supplies), the
FIT codec, and device control on the watch (when to hold the processor awake, when the heart
rate sensor runs).

## Use

Add this repository as a git submodule at `health-calculations/`, then in
`settings.gradle.kts`:

```kotlin
includeBuild("health-calculations")
```

and depend on it:

```kotlin
dependencies {
    implementation("tech.mmarca.healthcalculations:health-calculations")
    // The Wear OS sleep-minute fixtures, for tests that feed the sleep estimators.
    testImplementation(testFixtures("tech.mmarca.healthcalculations:health-calculations"))
}
```

A change here reaches an app when the app moves its submodule to the new commit. Move both
apps together, so the two give the same number.

## Build

JDK 17 or 21.

```bash
./gradlew verifyCi
```

runs the unit tests, including the file-size and function-length ratchets the apps carry.

## License

health-calculations is free software under the GNU Affero General Public License, version 3
or (at your option) any later version (AGPL-3.0-or-later), the same license as OpenVitals and
OpenWatch. See [LICENSE](LICENSE). An app that includes the library is a work based on it.

## Wiki

The [wiki](https://github.com/Hale-MTU/health-calculations/wiki) mirrors this repository's Markdown on every push to `main` (`.github/workflows/wiki.yml`, `scripts/sync-wiki.py`, the same script as in OpenVitals, OpenWatch, OpenLink and OpenBridge). Edit the files here; the wiki is overwritten. `python3 scripts/sync-wiki.py --repo Hale-MTU/health-calculations --check` fails on a relative link to a file that does not exist, and runs in the verify workflow.
