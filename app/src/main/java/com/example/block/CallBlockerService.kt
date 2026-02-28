package com.example.block

import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import android.telecom.Call

class CallBlockerService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val incomingNumber = callDetails.handle?.schemeSpecificPart
        val shouldBlock = BlockedNumberStore(this).isBlocked(incomingNumber)

        val response = if (shouldBlock) {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
                .build()
        } else {
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        }

        respondToCall(callDetails, response)
    }
}
