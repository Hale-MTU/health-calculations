package tech.mmarca.healthcalculations.architecture

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * A ratchet on file length, as in OpenVitals and OpenWatch: no new file passes [MaxLines], and a
 * listed file may shrink, never grow. When this fails on a new file, split the file. When it
 * fails on a listed file, move the new code out. When you shrink a listed file, lower its ceiling
 * or remove it in the same commit.
 */
class FileSizeRatchetTest {

    private val lineCounts: Map<String, Int> by lazy {
        File(SourceRoot).walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .associate { it.relativeTo(File(SourceRoot)).invariantSeparatorsPath to it.readLines().size }
    }

    @Test
    fun `no new file passes the limit`() {
        val unlisted = lineCounts.filter { (path, lines) -> lines > MaxLines && path !in Ceilings }

        assertWithMessage("files over $MaxLines lines that are not listed. Split them: $unlisted")
            .that(unlisted)
            .isEmpty()
    }

    @Test
    fun `a listed file does not grow past its ceiling`() {
        val grown = Ceilings.filter { (path, ceiling) -> (lineCounts[path] ?: 0) > ceiling }
            .mapValues { (path, ceiling) -> "${lineCounts[path]} lines, ceiling $ceiling" }

        assertWithMessage("listed files grew past their ceiling. Move the new code out: $grown")
            .that(grown)
            .isEmpty()
    }

    @Test
    fun `the list holds only files that still need it`() {
        // A stale entry would let a file grow back unseen.
        val stale = Ceilings.keys.filter { path -> (lineCounts[path] ?: 0) <= MaxLines }

        assertWithMessage("listed files are gone or under $MaxLines lines. Remove them from the list: $stale")
            .that(stale)
            .isEmpty()
    }

    private companion object {
        const val SourceRoot = "src/main/kotlin/tech/mmarca/healthcalculations"
        const val MaxLines = 800

        /** The ceiling OpenVitals gave the file, which came with it. */
        val Ceilings: Map<String, Int> = mapOf(
            "insights/BodyEnergyTimeline.kt" to 1050,
        )
    }
}
