package com.example.block

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow

object InCallStateStore {
    val currentCall = MutableStateFlow<Call?>(null)
}
