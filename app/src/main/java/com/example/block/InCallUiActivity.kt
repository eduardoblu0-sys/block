package com.example.block

import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.VideoProfile
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.example.block.ui.theme.BlockTheme

class InCallUiActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        setContent {
            BlockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    InCallScreen(modifier = Modifier.padding(innerPadding)) {
                        finish()
                    }
                }
            }
        }
    }
}

@Composable
private fun InCallScreen(modifier: Modifier = Modifier, onClose: () -> Unit) {
    val call by InCallStateStore.currentCall.collectAsState()

    if (call == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.in_call_no_active_call),
                style = MaterialTheme.typography.titleMedium
            )
            Button(onClick = onClose) {
                Text(stringResource(R.string.in_call_close))
            }
        }
        return
    }

    val details = call?.details
    val number = details?.handle?.schemeSpecificPart ?: stringResource(R.string.in_call_unknown_number)
    val stateText = describeState(call?.state ?: Call.STATE_NEW)
    val isRinging = call?.state == Call.STATE_RINGING
    val isActive = call?.state == Call.STATE_ACTIVE

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.in_call_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(R.string.in_call_state, stateText),
            style = MaterialTheme.typography.bodyLarge
        )

        if (isRinging) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = {
                    call?.answer(VideoProfile.STATE_AUDIO_ONLY)
                }) {
                    Text(stringResource(R.string.in_call_answer))
                }
                Button(onClick = {
                    call?.reject(false, null)
                }) {
                    Text(stringResource(R.string.in_call_reject))
                }
            }
        }

        if (isActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = {
                    call?.disconnect()
                }) {
                    Text(stringResource(R.string.in_call_hang_up))
                }
            }
        }

        Button(onClick = onClose) {
            Text(stringResource(R.string.in_call_close))
        }
    }
}

private fun describeState(state: Int): String {
    return when (state) {
        Call.STATE_NEW -> "novo"
        Call.STATE_RINGING -> "tocando"
        Call.STATE_DIALING -> "discando"
        Call.STATE_CONNECTING -> "conectando"
        Call.STATE_ACTIVE -> "em andamento"
        Call.STATE_HOLDING -> "em espera"
        Call.STATE_DISCONNECTING -> "desconectando"
        Call.STATE_DISCONNECTED -> "encerrada"
        else -> "desconhecido"
    }
}
