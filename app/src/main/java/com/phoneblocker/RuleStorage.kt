package com.phoneblocker

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RuleStorage {
    private const val PREFS_NAME = "phone_blocker_prefs"
    private const val RULES_KEY = "block_rules"
    
    private val gson = Gson()
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveRules(context: Context, rules: List<BlockRule>) {
        val json = gson.toJson(rules)
        getPrefs(context).edit().putString(RULES_KEY, json).apply()
    }
    
    fun loadRules(context: Context): MutableList<BlockRule> {
        val json = getPrefs(context).getString(RULES_KEY, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<BlockRule>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
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
    
    fun findMatchingRule(context: Context, phoneNumber: String): BlockRule? {
        val rules = loadRules(context)
        return rules.find { it.matches(phoneNumber) }
    }
}
