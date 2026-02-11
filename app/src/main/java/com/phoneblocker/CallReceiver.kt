package com.phoneblocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import android.os.Build

class CallReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "CallReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        
        Log.d(TAG, "Phone state: $state, number: $phoneNumber")
        
        if (state == TelephonyManager.EXTRA_STATE_RINGING && phoneNumber != null) {
            val (matchedRule, isAllowed) = RuleStorage.findMatchingRuleForLog(context, phoneNumber)
            
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
            CallLogStorage.addEntry(context, logEntry)
            
            if (!isAllowed && matchedRule != null) {
                Log.d(TAG, "Blocking call from $phoneNumber (rule: ${matchedRule.pattern}, action: ${matchedRule.action})")
                
                when (matchedRule.action) {
                    BlockRule.Action.BLOCK,
                    BlockRule.Action.VOICEMAIL,
                    BlockRule.Action.SILENCE -> {
                        endCall(context)
                    }
                    BlockRule.Action.ALLOW -> {
                        // Should not reach here
                        Log.d(TAG, "Allowing call from $phoneNumber")
                    }
                }
            } else {
                Log.d(TAG, "Allowing call from $phoneNumber" + 
                    if (matchedRule != null) " (ALLOW rule: ${matchedRule.pattern})" else " (no matching rule)")
            }
        }
    }
    
    private fun endCall(context: Context) {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                telecomManager.endCall()
                Log.d(TAG, "Call ended via TelecomManager")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException ending call: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error ending call: ${e.message}")
        }
    }
}
