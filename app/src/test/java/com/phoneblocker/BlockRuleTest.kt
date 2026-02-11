package com.phoneblocker

import org.junit.Assert.*
import org.junit.Test

class BlockRuleTest {

    @Test
    fun `pattern with trailing wildcards matches correct length`() {
        val rule = BlockRule(pattern = "3456****", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("34561234"))
        assertTrue(rule.matches("34560000"))
        assertTrue(rule.matches("34569999"))
    }

    @Test
    fun `pattern rejects wrong length numbers`() {
        val rule = BlockRule(pattern = "3456****", action = BlockRule.Action.BLOCK)
        
        assertFalse(rule.matches("3456123"))    // too short
        assertFalse(rule.matches("345612345"))  // too long
        assertFalse(rule.matches("3456"))       // way too short
    }

    @Test
    fun `pattern rejects wrong prefix`() {
        val rule = BlockRule(pattern = "3456****", action = BlockRule.Action.BLOCK)
        
        assertFalse(rule.matches("12341234"))
        assertFalse(rule.matches("34571234"))
        assertFalse(rule.matches("00001234"))
    }

    @Test
    fun `pattern with leading wildcards`() {
        val rule = BlockRule(pattern = "****1234", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("00001234"))
        assertTrue(rule.matches("99991234"))
        assertTrue(rule.matches("56781234"))
        assertFalse(rule.matches("00005678"))
    }

    @Test
    fun `pattern with middle wildcards`() {
        val rule = BlockRule(pattern = "34**5678", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("34005678"))
        assertTrue(rule.matches("34995678"))
        assertFalse(rule.matches("34001234"))
        assertFalse(rule.matches("12005678"))
    }

    @Test
    fun `exact number match without wildcards`() {
        val rule = BlockRule(pattern = "12345678", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("12345678"))
        assertFalse(rule.matches("12345679"))
        assertFalse(rule.matches("12345677"))
    }

    @Test
    fun `all wildcards matches any number of same length`() {
        val rule = BlockRule(pattern = "********", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("00000000"))
        assertTrue(rule.matches("99999999"))
        assertTrue(rule.matches("12345678"))
        assertFalse(rule.matches("1234567"))   // wrong length
        assertFalse(rule.matches("123456789")) // wrong length
    }

    @Test
    fun `disabled rule does not match`() {
        val rule = BlockRule(pattern = "3456****", action = BlockRule.Action.BLOCK, enabled = false)
        
        assertFalse(rule.matches("34561234"))
    }

    @Test
    fun `matches with country code prefix`() {
        val rule = BlockRule(pattern = "34561234", action = BlockRule.Action.BLOCK)
        
        // Should match with various country code formats
        assertTrue(rule.matches("+134561234"))   // +1 (US)
        assertTrue(rule.matches("+8634561234"))  // +86 (China)
    }

    @Test
    fun `matches strips non-digit characters`() {
        val rule = BlockRule(pattern = "34561234", action = BlockRule.Action.BLOCK)
        
        assertTrue(rule.matches("3456-1234"))
        assertTrue(rule.matches("3456 1234"))
        assertTrue(rule.matches("(345) 612-34"))
    }

    @Test
    fun `ten digit pattern for toll-free numbers`() {
        val rule = BlockRule(pattern = "1800******", action = BlockRule.Action.VOICEMAIL)
        
        assertTrue(rule.matches("1800123456"))
        assertTrue(rule.matches("1800000000"))
        assertFalse(rule.matches("1801123456"))
        assertFalse(rule.matches("180012345"))  // too short
    }

    @Test
    fun `different actions are stored correctly`() {
        val blockRule = BlockRule(pattern = "1234", action = BlockRule.Action.BLOCK)
        val silenceRule = BlockRule(pattern = "1234", action = BlockRule.Action.SILENCE)
        val voicemailRule = BlockRule(pattern = "1234", action = BlockRule.Action.VOICEMAIL)
        
        assertEquals(BlockRule.Action.BLOCK, blockRule.action)
        assertEquals(BlockRule.Action.SILENCE, silenceRule.action)
        assertEquals(BlockRule.Action.VOICEMAIL, voicemailRule.action)
    }
}
