package com.phoneblocker

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

class CallBlockerService : CallScreeningService() {
    
    companion object {
        private const val TAG = "CallBlockerService"
    }
    
    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        Log.d(TAG, "Screening call from: $phoneNumber")
        
        val (matchedRule, isAllowed) = RuleStorage.findMatchingRuleForLog(this, phoneNumber)
        
        // Determine if it matched an ALLOW rule or just no rule matched
        val matchedAllowRule = isAllowed && matchedRule != null
        
        // Log the call
        val logEntry = CallLogEntry(
            rawNumber = phoneNumber,
            timestamp = System.currentTimeMillis(),
            wasBlocked = !isAllowed,
            matchedPattern = matchedRule?.pattern,
            action = matchedRule?.action?.name,
            matchedAllowRule = matchedAllowRule
        )
        CallLogStorage.addEntry(this, logEntry)
        
        val response = if (!isAllowed && matchedRule != null) {
            Log.d(TAG, "Blocking call (rule: ${matchedRule.pattern}, action: ${matchedRule.action})")
            
            when (matchedRule.action) {
                BlockRule.Action.BLOCK -> {
                    CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(true)
                        .build()
                }
                BlockRule.Action.SILENCE -> {
                    CallResponse.Builder()
                        .setDisallowCall(false)
                        .setRejectCall(false)
                        .setSilenceCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                        .build()
                }
                BlockRule.Action.VOICEMAIL -> {
                    CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                        .build()
                }
                BlockRule.Action.ALLOW -> {
                    // Should not reach here
                    CallResponse.Builder()
                        .setDisallowCall(false)
                        .setRejectCall(false)
                        .build()
                }
            }
        } else {
            Log.d(TAG, "Allowing call" + 
                if (matchedRule != null) " (ALLOW rule: ${matchedRule.pattern})" else " (no matching rule)")
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()
        }
        
        respondToCall(callDetails, response)
    }
}
