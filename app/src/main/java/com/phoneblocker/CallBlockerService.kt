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
        
        val matchingRule = RuleStorage.findMatchingRule(this, phoneNumber)
        
        // Log the call
        val logEntry = CallLogEntry(
            rawNumber = phoneNumber,
            timestamp = System.currentTimeMillis(),
            wasBlocked = matchingRule != null,
            matchedPattern = matchingRule?.pattern,
            action = matchingRule?.action?.name
        )
        CallLogStorage.addEntry(this, logEntry)
        
        val response = if (matchingRule != null) {
            Log.d(TAG, "Blocking call (rule: ${matchingRule.pattern}, action: ${matchingRule.action})")
            
            when (matchingRule.action) {
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
            }
        } else {
            Log.d(TAG, "Allowing call (no matching rule)")
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()
        }
        
        respondToCall(callDetails, response)
    }
}
