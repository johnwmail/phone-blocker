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
        
        val response = if (matchingRule != null) {
            Log.d(TAG, "Found matching rule: ${matchingRule.pattern} -> ${matchingRule.action}")
            
            when (matchingRule.action) {
                BlockRule.Action.BLOCK -> {
                    // Reject the call completely
                    CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(true)
                        .build()
                }
                BlockRule.Action.SILENCE -> {
                    // Silence the call (no ring), but don't reject
                    // Call will still appear and can go to voicemail
                    CallResponse.Builder()
                        .setDisallowCall(false)
                        .setRejectCall(false)
                        .setSilenceCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                        .build()
                }
                BlockRule.Action.VOICEMAIL -> {
                    // Reject call so it goes to voicemail
                    CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                        .build()
                }
            }
        } else {
            // No matching rule - allow the call
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()
        }
        
        respondToCall(callDetails, response)
    }
}
