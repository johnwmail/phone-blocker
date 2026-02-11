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
            val matchingRule = RuleStorage.findMatchingRule(context, phoneNumber)
            
            // Log the call
            val logEntry = CallLogEntry(
                rawNumber = phoneNumber,
                timestamp = System.currentTimeMillis(),
                wasBlocked = matchingRule != null,
                matchedPattern = matchingRule?.pattern,
                action = matchingRule?.action?.name
            )
            CallLogStorage.addEntry(context, logEntry)
            
            if (matchingRule != null) {
                Log.d(TAG, "Blocking call from $phoneNumber (rule: ${matchingRule.pattern})")
                
                when (matchingRule.action) {
                    BlockRule.Action.BLOCK,
                    BlockRule.Action.VOICEMAIL -> {
                        endCall(context)
                    }
                    BlockRule.Action.SILENCE -> {
                        endCall(context)
                    }
                }
            } else {
                Log.d(TAG, "Allowing call from $phoneNumber (no matching rule)")
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
