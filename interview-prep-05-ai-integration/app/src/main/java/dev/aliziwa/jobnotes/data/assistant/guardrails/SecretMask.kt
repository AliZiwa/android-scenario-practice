package dev.aliziwa.jobnotes.data.assistant.guardrails

class SecretMask(
    private val value: String
) {
    override fun toString(): String = "***"
}