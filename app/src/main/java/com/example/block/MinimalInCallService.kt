package com.example.block

import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService

/**
 * Serviço de chamadas que assume responsabilidade de UI e toque ao atuar
 * como app de telefone padrão.
 */
class MinimalInCallService : InCallService() {

    private var activeCall: Call? = null
    private var activeRingingCall: Call? = null
    private var ringtone: Ringtone? = null

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            when (state) {
                Call.STATE_RINGING -> startRinging(call)
                Call.STATE_ACTIVE,
                Call.STATE_CONNECTING,
                Call.STATE_DIALING,
                Call.STATE_DISCONNECTED,
                Call.STATE_DISCONNECTING -> stopRingingIfMatches(call)
            }

            if (state == Call.STATE_DISCONNECTED) {
                InCallStateStore.currentCall.value = null
                if (activeCall == call) {
                    activeCall = null
                }
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        activeCall?.unregisterCallback(callCallback)
        activeCall = call

        call.registerCallback(callCallback)
        InCallStateStore.currentCall.value = call

        if (call.state == Call.STATE_RINGING) {
            startRinging(call)
        }

        launchInCallUi()
    }

    override fun onCallRemoved(call: Call) {
        stopRingingIfMatches(call)
        call.unregisterCallback(callCallback)

        if (activeCall == call) {
            activeCall = null
            InCallStateStore.currentCall.value = null
        }

        super.onCallRemoved(call)
    }

    override fun onDestroy() {
        activeCall?.unregisterCallback(callCallback)
        activeCall = null
        InCallStateStore.currentCall.value = null
        stopRinging()
        super.onDestroy()
    }

    private fun launchInCallUi() {
        val uiIntent = Intent(this, InCallUiActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(uiIntent)
    }

    private fun startRinging(call: Call) {
        activeRingingCall = call

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val current = ringtone ?: RingtoneManager.getRingtone(applicationContext, uri).also {
            ringtone = it
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            current.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        }

        if (!current.isPlaying) {
            current.play()
        }
    }

    private fun stopRingingIfMatches(call: Call) {
        if (activeRingingCall == call) {
            stopRinging()
        }
    }

    private fun stopRinging() {
        ringtone?.stop()
        activeRingingCall = null
    }
}
