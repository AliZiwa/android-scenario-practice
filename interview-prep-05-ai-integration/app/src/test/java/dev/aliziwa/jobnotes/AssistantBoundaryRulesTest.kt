package dev.aliziwa.jobnotes

import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.io.File

/**
 * The AI boundary rules as a test. It reads the main source tree and fails on the things the README
 * says must not be there when you are done. Ignored on `main` because all of them are there now.
 *
 * Remove the @Ignore once the rules hold, and keep it green. Rename packages here if your layout
 * differs; the rules are the deliverable, the names are yours.
 *
 *  - No secret in source: nothing under `src/main` contains a string starting with `sk-`.
 *  - No secret in logs: no `Log.` call on a line that mentions `API_KEY`, `apiKey` or `Authorization`.
 *  - No process-wide mutable state for assistant results: no `object LastSummary` and no reference to it.
 *  - No unscoped work: no `GlobalScope` and no `CoroutineScope(` constructed inside `ui`.
 *  - `ui` imports nothing from `..data..`; it sees the assistant through `domain` only.
 */
@Ignore("Enable when the AI boundary rules hold")
class AssistantBoundaryRulesTest {

    private val root = File("src/main/java/dev/aliziwa/jobnotes")

    private fun sources(sub: String = ""): List<File> =
        File(root, sub).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun offendingLines(files: List<File>, predicate: (String) -> Boolean): List<String> =
        files.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line -> if (predicate(line)) "${f.name}:${i + 1}: ${line.trim()}" else null }
        }

    private fun assertNone(violations: List<String>, what: String) {
        assertTrue("$what:\n" + violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun `no secret in source`() {
        assertNone(offendingLines(sources()) { it.contains("\"sk-") }, "Key literal in source")
    }

    @Test
    fun `no secret in logs`() {
        assertNone(
            offendingLines(sources()) { line ->
                line.contains("Log.") && (line.contains("API_KEY") || line.contains("apiKey") || line.contains("Authorization"))
            },
            "Log line that can print the key",
        )
    }

    @Test
    fun `no process-wide assistant state`() {
        assertNone(offendingLines(sources()) { it.contains("LastSummary") }, "LastSummary still referenced")
    }

    @Test
    fun `no unscoped work in ui`() {
        assertNone(
            offendingLines(sources("ui")) { it.contains("GlobalScope") || it.contains("CoroutineScope(") },
            "Unscoped coroutine in ui",
        )
    }

    @Test
    fun `ui does not see data`() {
        val imports = offendingLines(sources("ui")) { it.startsWith("import dev.aliziwa.jobnotes.data") }
        assertNone(imports, "ui imports data")
    }
}
