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
            Log.d(TAG, "Processing call (rule: ${matchedRule.pattern}, action: ${matchedRule.action})")
            
            when (matchedRule.action) {
                BlockRule.Action.BLOCK -> {
                    // Reject the call completely, skip notification
                    // Caller may hear busy or call drops
                    Log.d(TAG, "BLOCK: Rejecting call, skipping notification")
                    CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(true)
                        .build()
                }
                BlockRule.Action.SILENCE -> {
                    // Let the call through but silence the ringer
                    // Phone will show incoming call but won't ring
                    // User can still answer or decline manually
                    Log.d(TAG, "SILENCE: Silencing ringer, call still visible")
                    CallResponse.Builder()
                        .setDisallowCall(false)
                        .setRejectCall(false)
                        .setSilenceCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                        .build()
                }
                BlockRule.Action.VOICEMAIL -> {
                    // Decline the call (like pressing Decline button)
                    // This routes to voicemail if carrier supports it
                    // Show notification so user knows a call was declined
                    Log.d(TAG, "VOICEMAIL: Declining call (routes to voicemail)")
                    CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)  // Important: true = decline to voicemail
                        .setSkipCallLog(false)
                        .setSkipNotification(false)  // Show missed call notification
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
