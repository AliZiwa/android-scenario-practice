package dev.aliziwa.jobnotes.data.assistant.guardrails

object PromptSanitizer {
    // Regex patterns for common PII
    private val PHONE_REGEX = Regex(
            """(\+?\d{1,3}[-.\s]?)?(\(?\d{3}\)?[-.\s]?)?\d{3}[-.\s]?\d{4}\b"""
        )
    private val EMAIL_REGEX = Regex(
        """[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}\b"""
    )

    fun sanitize(input: String): String {
        var result = input
        result = PHONE_REGEX.replace(result, "[PHONE]")
        result = EMAIL_REGEX.replace(result, "[EMAIL]")
        return result
    }
}