package com.example.jobsheetbook.util

object PhoneNormalizer {

    sealed interface Result {
        object Blank : Result
        data class Valid(val stored: String) : Result
        object Invalid : Result
    }

    private val ALLOWED_CHARS = setOf('+', ' ', '-', '.', '(', ')')

    fun normalize(input: String?): Result {
        if (input == null) return Result.Blank
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Result.Blank

        // Rule 1: Allow only digits, one leading "+", spaces, hyphens, dots, and parentheses.
        // Any other character makes the result Invalid.
        for (i in trimmed.indices) {
            val c = trimmed[i]
            if (c.isDigit()) continue
            if (c in ALLOWED_CHARS) {
                if (c == '+' && i != 0) return Result.Invalid
                continue
            }
            return Result.Invalid
        }

        val startsWithPlus = trimmed.startsWith("+")

        // Remove spaces, hyphens, dots, and parentheses before the next steps.
        val cleaned = trimmed.filter { it.isDigit() }
        if (cleaned.isEmpty()) return Result.Invalid

        // Rule 3: If the input starts with "+", or its digits start with "00", the number is marked international.
        // Drop the "+" or the leading "00".
        if (startsWithPlus || cleaned.startsWith("00")) {
            val remainingDigits = if (startsWithPlus) cleaned else cleaned.removePrefix("00")
            return when {
                remainingDigits.length == 12 && remainingDigits.startsWith("91") -> {
                    Result.Valid(remainingDigits.takeLast(10))
                }
                remainingDigits.length in 11..15 && !remainingDigits.startsWith("91") -> {
                    Result.Valid(remainingDigits)
                }
                else -> Result.Invalid
            }
        }

        // Rule 4: Otherwise (no "+" and no "00"), these are the only accepted forms. Anything else is Invalid:
        return when {
            cleaned.length == 10 -> Result.Valid(cleaned)
            cleaned.length == 11 && cleaned.startsWith("0") -> Result.Valid(cleaned.drop(1))
            cleaned.length == 12 && cleaned.startsWith("91") -> Result.Valid(cleaned.takeLast(10))
            cleaned.length == 13 && cleaned.startsWith("091") -> Result.Valid(cleaned.takeLast(10))
            else -> Result.Invalid
        }
    }

    private fun resolveStoredDigits(stored: String?): String? {
        if (stored.isNullOrBlank()) return null
        when (val res = normalize(stored)) {
            is Result.Valid -> return res.stored
            else -> {
                val trimmed = stored.trim()
                if (trimmed.all { it.isDigit() } && trimmed.length in 11..15) {
                    return trimmed
                }
                return null
            }
        }
    }

    fun formatForDisplay(stored: String?): String {
        if (stored == null) return ""
        val digits = resolveStoredDigits(stored) ?: return stored
        return when (digits.length) {
            10 -> "${digits.substring(0, 5)} ${digits.substring(5)}"
            in 11..15 -> "+$digits"
            else -> stored
        }
    }

    fun toWhatsAppDigits(stored: String?): String? {
        if (stored == null) return null
        val digits = resolveStoredDigits(stored) ?: return null
        return when {
            digits.length == 10 -> {
                if (digits[0] in '6'..'9') "91$digits" else null
            }
            digits.length in 11..15 -> digits
            else -> null
        }
    }

    fun toDialNumber(stored: String?): String? {
        if (stored == null) return null
        val digits = resolveStoredDigits(stored) ?: return null
        return when {
            digits.length == 10 -> "+91$digits"
            digits.length in 11..15 -> "+$digits"
            else -> null
        }
    }
}
