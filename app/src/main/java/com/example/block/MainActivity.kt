package com.example.block

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.block.ui.theme.BlockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BlockApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun BlockApp(modifier: Modifier = Modifier) {
    var showHistory by remember { mutableStateOf(false) }

    if (showHistory) {
        IncomingCallHistoryScreen(
            modifier = modifier,
            onBack = { showHistory = false }
        )
    } else {
        HomeScreen(
            modifier = modifier,
            onOpenHistory = { showHistory = true }
        )
    }
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier, onOpenHistory: () -> Unit) {
    val context = LocalContext.current
    val blockedStore = remember { BlockedNumberStore(context) }
    var numberInput by remember { mutableStateOf("") }
    var blockedNumbers by remember { mutableStateOf(blockedStore.getBlockedNumbers().toList().sorted()) }
    var isDefaultDialer by remember { mutableStateOf(isDefaultDialerApp(context)) }
    var hasCallPermission by remember { mutableStateOf(hasCallPhonePermission(context)) }

    fun refreshBlockedNumbers() {
        blockedNumbers = blockedStore.getBlockedNumbers().toList().sorted()
    }

    fun refreshAppRoleState() {
        isDefaultDialer = isDefaultDialerApp(context)
        hasCallPermission = hasCallPhonePermission(context)
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCallPermission = granted
    }

    LaunchedEffect(Unit) {
        refreshAppRoleState()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.home_description),
            style = MaterialTheme.typography.bodyLarge
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.default_phone_app_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isDefaultDialer) {
                        stringResource(R.string.default_phone_app_enabled)
                    } else {
                        stringResource(R.string.default_phone_app_disabled)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        requestDefaultDialerRole(context)
                        refreshAppRoleState()
                    }) {
                        Text(stringResource(R.string.request_default_phone_app))
                    }
                    Button(onClick = {
                        if (hasCallPermission) {
                            openDialer(context)
                        } else {
                            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                        }
                    }) {
                        Text(
                            text = if (hasCallPermission) {
                                stringResource(R.string.open_phone)
                            } else {
                                stringResource(R.string.grant_call_permission)
                            }
                        )
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.call_blocker_card_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.call_blocker_card_body),
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = numberInput,
                    onValueChange = { numberInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.number_input_label)) },
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (blockedStore.addNumber(numberInput)) {
                            numberInput = ""
                            refreshBlockedNumbers()
                        }
                    }) {
                        Text(text = stringResource(R.string.block_number))
                    }
                    Button(onClick = {
                        openCallScreeningSettings(context)
                    }) {
                        Text(text = stringResource(R.string.open_screening_settings))
                    }
                }

                if (blockedNumbers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_blocked_numbers),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = stringResource(R.string.blocked_list_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    blockedNumbers.forEach { number ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = number, style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = {
                                blockedStore.removeNumber(number)
                                refreshBlockedNumbers()
                            }) {
                                Text(text = stringResource(R.string.unblock_number))
                            }
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_card_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.home_card_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onOpenHistory) {
                    Text(text = stringResource(R.string.home_open_history))
                }
            }
        }
    }
}

@Composable
fun IncomingCallHistoryScreen(modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val repository = remember { CallHistoryRepository(context.contentResolver) }
    val blockedStore = remember { BlockedNumberStore(context) }

    var hasPermission by remember { mutableStateOf(hasReadCallLogPermission(context)) }
    var history by remember { mutableStateOf(emptyList<CallEntry>()) }
    var blockedNumbers by remember { mutableStateOf(blockedStore.getBlockedNumbers()) }

    fun refreshHistory() {
        history = if (hasPermission) repository.getIncomingHistory() else emptyList()
    }

    fun refreshBlockedNumbers() {
        blockedNumbers = blockedStore.getBlockedNumbers()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        refreshHistory()
    }

    LaunchedEffect(hasPermission) {
        refreshHistory()
        refreshBlockedNumbers()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.title_call_history),
            style = MaterialTheme.typography.headlineSmall
        )

        if (!hasPermission) {
            Text(
                text = stringResource(R.string.permission_explanation),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_CALL_LOG) }) {
                Text(stringResource(R.string.grant_permission))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onBack != null) {
                Button(onClick = onBack) {
                    Text(stringResource(R.string.back))
                }
            }
            Button(onClick = { refreshHistory() }, enabled = hasPermission) {
                Text(stringResource(R.string.refresh))
            }
        }

        when {
            !hasPermission -> Text(stringResource(R.string.no_permission))
            history.isEmpty() -> Text(stringResource(R.string.empty_history))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history) { entry ->
                    Column {
                        Text(text = entry.number, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(
                                R.string.received_at,
                                CallHistoryRepository.formatDateTime(entry.receivedAt)
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.duration_seconds, entry.durationSeconds),
                            style = MaterialTheme.typography.bodySmall
                        )

                        val isBlocked = blockedNumbers.contains(BlockedNumberStore.normalize(entry.number))
                        Button(
                            onClick = {
                                if (blockedStore.addNumber(entry.number)) {
                                    refreshBlockedNumbers()
                                }
                            },
                            enabled = !isBlocked
                        ) {
                            Text(
                                if (isBlocked) {
                                    stringResource(R.string.number_already_blocked)
                                } else {
                                    stringResource(R.string.block_number)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun requestDefaultDialerRole(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleAvailable(RoleManager.ROLE_DIALER) == true &&
            !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        ) {
            context.startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            return
        }
    }

    context.startActivity(
        Intent(TelecomManagerCompat.ACTION_CHANGE_DEFAULT_DIALER).putExtra(
            TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
            context.packageName
        )
    )
}

private fun openDialer(context: Context) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:"))
    context.startActivity(intent)
}

private fun openCallScreeningSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true &&
            !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        ) {
            context.startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
            return
        }
    }

    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
}

private fun isDefaultDialerApp(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
    } else {
        false
    }
}

private fun hasReadCallLogPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CALL_LOG
    ) == PackageManager.PERMISSION_GRANTED
}

private fun hasCallPhonePermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED
}

private object TelecomManagerCompat {
    const val ACTION_CHANGE_DEFAULT_DIALER = TelecomManager.ACTION_CHANGE_DEFAULT_DIALER
}
