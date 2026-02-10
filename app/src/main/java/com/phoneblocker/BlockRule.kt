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
     * Pattern uses * as single digit wildcard.
     * Example: "3456****" matches any 8-digit number starting with 3456
     */
    fun matches(phoneNumber: String): Boolean {
        if (!enabled) return false
        
        // Clean the phone number (remove spaces, dashes, etc)
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        
        // Handle patterns with country code flexibility
        val numbersToCheck = mutableListOf(cleanNumber)
        
        // If number starts with +, also check without country code
        if (cleanNumber.startsWith("+")) {
            // Try removing +1 (US), +44 (UK), etc.
            if (cleanNumber.length > 2) {
                numbersToCheck.add(cleanNumber.substring(2)) // Remove +X
            }
            if (cleanNumber.length > 3) {
                numbersToCheck.add(cleanNumber.substring(3)) // Remove +XX
            }
            if (cleanNumber.length > 4) {
                numbersToCheck.add(cleanNumber.substring(4)) // Remove +XXX
            }
        }
        
        // Also add the raw number without +
        if (cleanNumber.startsWith("+")) {
            numbersToCheck.add(cleanNumber.substring(1))
        }
        
        return numbersToCheck.any { matchesPattern(it, pattern) }
    }
    
    private fun matchesPattern(number: String, pattern: String): Boolean {
        if (number.length != pattern.length) return false
        
        for (i in pattern.indices) {
            val p = pattern[i]
            val n = number[i]
            
            if (p == '*') continue  // Wildcard matches any digit
            if (p != n) return false
        }
        
        return true
    }
}
