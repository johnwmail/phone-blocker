package com.phoneblocker

import java.util.UUID

data class BlockRule(
    val id: String = UUID.randomUUID().toString(),
    val pattern: String,
    val action: Action,
    val enabled: Boolean = true
) {
    enum class Action {
        BLOCK,           // Reject the call completely
        SILENCE,         // Don't ring, let it go to voicemail
        VOICEMAIL        // Send directly to voicemail
    }

    /**
     * Check if a phone number matches this rule's pattern.
     * 
     * Incoming number: all non-digits stripped (spaces, dashes, +, etc)
     * So +8613812345678 or 86-138-1234-5678 both become 8613812345678
     * 
     * Pattern wildcards:
     *   * = match zero or more digits
     *   ? = match exactly one digit
     * 
     * Examples:
     *   "86*" matches any number starting with 86 (China)
     *   "1*" matches any number starting with 1 (US/Canada)
     *   "138????????" matches 11-digit numbers starting with 138
     */
    fun matches(phoneNumber: String): Boolean {
        if (!enabled) return false
        
        // Clean the phone number: keep only digits (0-9)
        // This strips +, spaces, dashes, parentheses, etc.
        val cleanNumber = phoneNumber.filter { it.isDigit() }
        
        if (cleanNumber.isEmpty()) return false
        
        return matchesPattern(cleanNumber, pattern)
    }
    
    private fun matchesPattern(number: String, pattern: String): Boolean {
        // Convert pattern to regex
        // ? = exactly one digit [0-9]
        // * = zero or more digits [0-9]*
        // digits = literal digits
        
        val regexPattern = buildString {
            append("^")
            for (char in pattern) {
                when (char) {
                    '*' -> append("[0-9]*")
                    '?' -> append("[0-9]")
                    in '0'..'9' -> append(char)
                    else -> { } // Ignore other characters in pattern
                }
            }
            append("$")
        }
        
        return try {
            Regex(regexPattern).matches(number)
        } catch (e: Exception) {
            false
        }
    }
}
