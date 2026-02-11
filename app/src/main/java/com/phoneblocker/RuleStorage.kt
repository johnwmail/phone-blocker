package com.phoneblocker

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RuleStorage {
    private const val PREFS_NAME = "phone_blocker_prefs"
    private const val KEY_RULES = "block_rules"
    
    private val gson = Gson()
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun loadRules(context: Context): MutableList<BlockRule> {
        val json = getPrefs(context).getString(KEY_RULES, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<BlockRule>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }
    
    fun saveRules(context: Context, rules: List<BlockRule>) {
        val json = gson.toJson(rules)
        getPrefs(context).edit().putString(KEY_RULES, json).apply()
    }
    
    fun addRule(context: Context, rule: BlockRule) {
        val rules = loadRules(context)
        rules.add(rule)
        saveRules(context, rules)
    }
    
    fun removeRule(context: Context, ruleId: String) {
        val rules = loadRules(context)
        rules.removeAll { it.id == ruleId }
        saveRules(context, rules)
    }
    
    fun updateRule(context: Context, rule: BlockRule) {
        val rules = loadRules(context)
        val index = rules.indexOfFirst { it.id == rule.id }
        if (index >= 0) {
            rules[index] = rule
            saveRules(context, rules)
        }
    }
    
    /**
     * Find the matching rule for a phone number.
     * Priority: ALLOW rules are checked first (if any ALLOW matches, returns null to allow the call)
     * Then BLOCK/SILENCE/VOICEMAIL rules are checked.
     * 
     * @return The blocking rule to apply, or null if call should be allowed
     */
    fun findMatchingRule(context: Context, phoneNumber: String): BlockRule? {
        val rules = loadRules(context)
        
        // First, check ALLOW rules - if any match, allow the call (return null)
        val allowRules = rules.filter { it.action == BlockRule.Action.ALLOW && it.enabled }
        for (rule in allowRules) {
            if (rule.matches(phoneNumber)) {
                return null  // Allow the call
            }
        }
        
        // Then check blocking rules (BLOCK, SILENCE, VOICEMAIL)
        val blockingRules = rules.filter { it.action != BlockRule.Action.ALLOW && it.enabled }
        for (rule in blockingRules) {
            if (rule.matches(phoneNumber)) {
                return rule  // Block/silence/voicemail the call
            }
        }
        
        return null  // No matching rule, allow the call
    }
    
    /**
     * Find matching rule and return both the rule and whether it's an allow rule.
     * Used for logging purposes.
     */
    fun findMatchingRuleForLog(context: Context, phoneNumber: String): Pair<BlockRule?, Boolean> {
        val rules = loadRules(context)
        
        // First, check ALLOW rules
        val allowRules = rules.filter { it.action == BlockRule.Action.ALLOW && it.enabled }
        for (rule in allowRules) {
            if (rule.matches(phoneNumber)) {
                return Pair(rule, true)  // Matched an ALLOW rule
            }
        }
        
        // Then check blocking rules
        val blockingRules = rules.filter { it.action != BlockRule.Action.ALLOW && it.enabled }
        for (rule in blockingRules) {
            if (rule.matches(phoneNumber)) {
                return Pair(rule, false)  // Matched a blocking rule
            }
        }
        
        return Pair(null, true)  // No match, allowed by default
    }
}
