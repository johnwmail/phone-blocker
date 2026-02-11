package com.phoneblocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import android.os.Build
import android.os.Handler
import android.os.Looper

class CallReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "CallReceiver"
        private var originalRingerMode: Int = AudioManager.RINGER_MODE_NORMAL
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        
        Log.d(TAG, "Phone state: $state, number: $phoneNumber")
        
        // Restore ringer when call ends
        if (state == TelephonyManager.EXTRA_STATE_IDLE) {
            restoreRinger(context)
            return
        }
        
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
                Log.d(TAG, "Processing call from $phoneNumber (rule: ${matchedRule.pattern}, action: ${matchedRule.action})")
                
                when (matchedRule.action) {
                    BlockRule.Action.BLOCK -> {
                        // Reject immediately
                        Log.d(TAG, "BLOCK: Rejecting call immediately")
                        rejectCall(context)
                    }
                    BlockRule.Action.SILENCE -> {
                        // Mute the ringer - call still shows on screen
                        // User can still see and answer/decline
                        Log.d(TAG, "SILENCE: Muting ringer")
                        silenceRinger(context)
                    }
                    BlockRule.Action.VOICEMAIL -> {
                        // Decline the call (like pressing Decline button)
                        // This should route to voicemail if carrier supports it
                        Log.d(TAG, "VOICEMAIL: Declining call to voicemail")
                        rejectCall(context)
                    }
                    BlockRule.Action.ALLOW -> {
                        Log.d(TAG, "Allowing call from $phoneNumber")
                    }
                }
            } else {
                Log.d(TAG, "Allowing call from $phoneNumber" + 
                    if (matchedRule != null) " (ALLOW rule: ${matchedRule.pattern})" else " (no matching rule)")
            }
        }
    }
    
    private fun silenceRinger(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            // Save current mode to restore later
            originalRingerMode = audioManager.ringerMode
            // Mute the ringer
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            Log.d(TAG, "Ringer silenced (was: $originalRingerMode)")
        } catch (e: Exception) {
            Log.e(TAG, "Error silencing ringer: ${e.message}")
        }
    }
    
    private fun restoreRinger(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT && 
                originalRingerMode != AudioManager.RINGER_MODE_SILENT) {
                audioManager.ringerMode = originalRingerMode
                Log.d(TAG, "Ringer restored to: $originalRingerMode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring ringer: ${e.message}")
        }
    }
    
    private fun rejectCall(context: Context) {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // endCall() acts like pressing the Decline button
                // If carrier has voicemail, call will be routed there
                val result = telecomManager.endCall()
                Log.d(TAG, "Call rejected via TelecomManager: $result")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException rejecting call: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting call: ${e.message}")
        }
    }
}
