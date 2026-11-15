# AGENTS.md

The implementation guide for anyone, agent or person, working in this repository.

## What this is

The health calculations OpenVitals and OpenWatch share, as one Kotlin/JVM library. Both apps
take it as a git submodule at `health-calculations/` and a composite build
(`includeBuild("health-calculations")`), the way they take OpenLink. `README.md` lists the
packages.

## Invariants

Do not break these without an explicit decision.

- **Plain Kotlin, no dependency.** The main source set uses the Kotlin standard library and
  `java.*` only: no Android, no Health Connect, no coroutines, no I/O. The Wear OS app and
  `:wearlink` take the same code as the phones, and every calculation can be tested on a plain JVM.
- **Health Connect's constants are copied, all of them.** `model/ExerciseSessionRecord.kt` and
  `model/ExerciseSegment.kt` hold every exercise and segment type under Health Connect's names
  and values. Each app's `ExerciseTypeMirrorTest` compares them with the real classes, both
  ways. When the apps move to a Health Connect client that adds a type, add it here.
- **One formula, one place.** A calculation both apps need lives here and nowhere else. An app
  keeps what is its own: Health Connect reads and writes, screens, formatting, data catalogs.
  Data a calculation needs from the app comes in as a parameter or an interface the app
  implements (`CaffeineCatalog`), never as a copy.
- **Sourced or labelled.** Each formula names its source in the KDoc: the paper, the guideline
  or the standard. A threshold with no published source says "app heuristic".
- **Unknown is not zero.** A calculation without enough input returns null, a `NoData` result
  or a confidence that says so; it never returns NaN or a default that reads as a measurement.
  `stats/Stats.kt` holds the averages that keep that contract.
- **Behaviour changes reach both apps together.** A change here is released by moving the
  submodule in OpenVitals and OpenWatch. Say in the pull request what changes for the user, so
  both apps' changelogs can say it.
- **The ratchets hold.** `FileSizeRatchetTest` stops a file passing 800 lines (with a ceiling
  list) and `FunctionLengthRatchetTest` stops a new function passing 150. When one fails, fix
  the code, not the test.

## Rules

- Public API is what an app calls; the rest is `internal` or `private`. `internal` here means
  this library, not an app: a declaration either app uses must be public.
- Keep the package an app had for a file (`insights`, `model`, `cycle`, ...). A new file goes
  where the closest existing one is.
- Tests sit beside the code they test, under `src/test`, in the same package. A test that needs
  an app's data (a catalog, Health Connect) stays in that app.
- Text that leaves an app is structure, not data: nothing here logs, and an exception message
  never carries a value.

## Verification

`./gradlew verifyCi`: the unit tests and the ratchets. Before moving an app's submodule, run that
app's gates too (`./gradlew verifyCi` in OpenVitals; `./gradlew verifyCi verifyWearCi` in
OpenWatch).
