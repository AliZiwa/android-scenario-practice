package dev.aliziwa.partsbin

import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.io.File

/**
 * The dependency rule as a test. It reads the main source tree and fails if a file in a layer imports
 * something that layer is not allowed to see. Ignored on `main` because the layers do not exist yet.
 *
 * Remove the @Ignore once your packages match the names below, or rename the packages here to match
 * your ARCHITECTURE.md. The rule is the deliverable; the names are yours to choose.
 *
 *  - `domain` imports nothing from `android.*`, `androidx.*`, `retrofit2.*`, `okhttp3.*`, `androidx.room.*`,
 *    and nothing from `..data..` or `..ui..`.
 *  - `ui` imports nothing from `retrofit2.*`, `okhttp3.*`, `androidx.room.*`, and nothing from `..data.remote..`
 *    or `..data.local..`.
 *  - Nothing outside `data` imports `..data.remote..` or `..data.local..`.
 *  - No file imports `dev.aliziwa.partsbin.Globals`.
 */
@Ignore("Enable when the dependency rule holds")
class ArchitectureRulesTest {

    private val root = File("src/main/java/dev/aliziwa/partsbin")

    private fun sources(sub: String): List<File> =
        File(root, sub).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun imports(file: File): List<String> =
        file.readLines().filter { it.startsWith("import ") }.map { it.removePrefix("import ").trim() }

    private fun forbid(files: List<File>, vararg prefixes: String) {
        val violations = files.flatMap { f ->
            imports(f).filter { imp -> prefixes.any { imp.startsWith(it) } }.map { "${f.name}: $it" }
        }
        assertTrue("Forbidden imports:\n" + violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun `domain depends on nothing but the language`() {
        val files = sources("domain")
        assertTrue("No domain sources found", files.isNotEmpty())
        forbid(
            files,
            "android.", "androidx.", "retrofit2.", "okhttp3.",
            "dev.aliziwa.partsbin.data", "dev.aliziwa.partsbin.ui",
        )
    }

    @Test
    fun `ui does not see the network or the database`() {
        val files = sources("ui")
        assertTrue("No ui sources found", files.isNotEmpty())
        forbid(
            files,
            "retrofit2.", "okhttp3.", "androidx.room.",
            "dev.aliziwa.partsbin.data.remote", "dev.aliziwa.partsbin.data.local",
        )
    }

    @Test
    fun `nobody reaches into the data sources from outside data`() {
        val outside = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && !it.path.contains("/data/") }
            .toList()
        forbid(outside, "dev.aliziwa.partsbin.data.remote", "dev.aliziwa.partsbin.data.local")
    }

    @Test
    fun `globals is gone`() {
        val all = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        forbid(all, "dev.aliziwa.partsbin.Globals")
        assertTrue("Globals.kt still exists", !File(root, "Globals.kt").exists())
    }
}
