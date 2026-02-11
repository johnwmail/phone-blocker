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
            
            if (matchingRule != null) {
                Log.d(TAG, "Blocking call from $phoneNumber (rule: ${matchingRule.pattern})")
                
                when (matchingRule.action) {
                    BlockRule.Action.BLOCK,
                    BlockRule.Action.VOICEMAIL -> {
                        endCall(context)
                    }
                    BlockRule.Action.SILENCE -> {
                        // For silence, we let it ring but could mute
                        // Most effective is still to end the call
                        endCall(context)
                    }
                }
            }
        }
    }
    
    private fun endCall(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Android 9+ method
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                try {
                    telecomManager.endCall()
                    Log.d(TAG, "Call ended via TelecomManager")
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException ending call: ${e.message}")
                }
            } else {
                // Older method using reflection
                endCallViaReflection(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ending call: ${e.message}")
        }
    }
    
    @Suppress("DEPRECATION")
    private fun endCallViaReflection(context: Context) {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val clazz = Class.forName(telephonyManager.javaClass.name)
            val method = clazz.getDeclaredMethod("getITelephony")
            method.isAccessible = true
            val telephonyService = method.invoke(telephonyManager)
            val telephonyServiceClass = Class.forName(telephonyService.javaClass.name)
            val endCallMethod = telephonyServiceClass.getDeclaredMethod("endCall")
            endCallMethod.invoke(telephonyService)
            Log.d(TAG, "Call ended via reflection")
        } catch (e: Exception) {
            Log.e(TAG, "Reflection method failed: ${e.message}")
        }
    }
}
