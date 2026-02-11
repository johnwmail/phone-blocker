package com.phoneblocker

import org.junit.Assert.*
import org.junit.Test

class BlockRuleTest {

    @Test
    fun testStarWildcard_matchesZeroOrMoreDigits() {
        val rule = BlockRule(pattern = "138*", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("138"))           // Zero digits after
        assertTrue(rule.matches("1381"))          // One digit
        assertTrue(rule.matches("13812345678"))   // Many digits
        assertFalse(rule.matches("139"))          // Wrong prefix
        assertFalse(rule.matches("13"))           // Incomplete prefix
    }

    @Test
    fun testAllNonDigitsStripped() {
        val rule = BlockRule(pattern = "183234*", action = BlockRule.Action.BLOCK)
        
        // All these should match after stripping non-digits
        assertTrue(rule.matches("183+23456312"))   // + in middle stripped
        assertTrue(rule.matches("+18323456312"))   // + at start stripped
        assertTrue(rule.matches("183-234-5678"))   // dashes stripped
        assertTrue(rule.matches("183 234 5678"))   // spaces stripped
        assertTrue(rule.matches("(183) 234-5678")) // parentheses stripped
        assertTrue(rule.matches("18323456312"))    // clean number
    }

    @Test
    fun testCountryCodePatternChina() {
        val rule = BlockRule(pattern = "86*", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("+8613812345678"))  // + stripped
        assertTrue(rule.matches("8613812345678"))   // No +
        assertTrue(rule.matches("+86"))             // Just country code
        assertFalse(rule.matches("+1234567890"))    // US number (becomes 1234567890)
    }

    @Test
    fun testCountryCodePatternUS() {
        val rule = BlockRule(pattern = "1*", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("+14155551234"))
        assertTrue(rule.matches("14155551234"))
        assertFalse(rule.matches("+44123456789"))  // UK (becomes 44123456789)
    }

    @Test
    fun testQuestionWildcard_matchesExactlyOneDigit() {
        val rule = BlockRule(pattern = "138????", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("1380000"))       // Exactly 4 digits after
        assertTrue(rule.matches("1389999"))       // Exactly 4 digits after
        assertFalse(rule.matches("138000"))       // Only 3 digits after
        assertFalse(rule.matches("13800000"))     // 5 digits after
    }

    @Test
    fun testExactNumberMatch() {
        val rule = BlockRule(pattern = "13812345678", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("13812345678"))
        assertTrue(rule.matches("+13812345678"))   // + stripped
        assertTrue(rule.matches("138-1234-5678"))  // dashes stripped
        assertFalse(rule.matches("138123456789"))  // Extra digit
        assertFalse(rule.matches("1381234567"))    // Missing digit
    }

    @Test
    fun testMixedWildcards() {
        val rule = BlockRule(pattern = "138????*", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("1380000"))       // 4 digits after prefix, 0 more
        assertTrue(rule.matches("13800001234"))   // 4 digits + more
        assertFalse(rule.matches("138000"))       // Only 3 digits after
    }

    @Test
    fun testMatchAllPattern() {
        val rule = BlockRule(pattern = "*", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("1"))
        assertTrue(rule.matches("13812345678"))
        assertTrue(rule.matches("+8613812345678"))
    }

    @Test
    fun testEndingPattern() {
        val rule = BlockRule(pattern = "*1234", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("1234"))
        assertTrue(rule.matches("001234"))
        assertTrue(rule.matches("+861234"))       // + stripped -> 861234
        assertFalse(rule.matches("12345"))
    }

    @Test
    fun testMiddleWildcard() {
        val rule = BlockRule(pattern = "138*5678", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("1385678"))       // 0 digits in middle
        assertTrue(rule.matches("13805678"))      // 1 digit in middle
        assertTrue(rule.matches("138123455678"))  // Many digits in middle
        assertFalse(rule.matches("1385679"))      // Wrong ending
    }

    @Test
    fun testDisabledRuleDoesNotMatch() {
        val rule = BlockRule(pattern = "138*", action = BlockRule.Action.BLOCK, enabled = false)
        
        assertFalse(rule.matches("13812345678"))
    }

    @Test
    fun testTollFreePattern() {
        val rule = BlockRule(pattern = "1800*", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("18001234567"))
        assertTrue(rule.matches("+18001234567"))  // + stripped
        assertFalse(rule.matches("1888123456"))
    }

    @Test
    fun testActionTypesStored() {
        val blockRule = BlockRule(pattern = "123", action = BlockRule.Action.BLOCK)
        val silenceRule = BlockRule(pattern = "456", action = BlockRule.Action.SILENCE)
        val voicemailRule = BlockRule(pattern = "789", action = BlockRule.Action.VOICEMAIL)
        
        assertEquals(BlockRule.Action.BLOCK, blockRule.action)
        assertEquals(BlockRule.Action.SILENCE, silenceRule.action)
        assertEquals(BlockRule.Action.VOICEMAIL, voicemailRule.action)
    }

    @Test
    fun testFixedLengthChinaMobile() {
        // Match exactly 11 digit China mobile numbers starting with 138
        val rule = BlockRule(pattern = "138????????", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("13812345678"))   // 11 digits
        assertTrue(rule.matches("+13812345678"))  // + stripped
        assertTrue(rule.matches("138-1234-5678")) // dashes stripped
        assertFalse(rule.matches("13912345678"))  // Wrong prefix
        assertFalse(rule.matches("1381234567"))   // Only 10 digits
        assertFalse(rule.matches("138123456789")) // 12 digits
    }

    @Test
    fun testEmptyAndInvalidInput() {
        val rule = BlockRule(pattern = "138*", action = BlockRule.Action.BLOCK)
        
        assertFalse(rule.matches(""))             // Empty
        assertFalse(rule.matches("   "))          // Whitespace only
        assertFalse(rule.matches("+-"))           // No digits
    }
}
