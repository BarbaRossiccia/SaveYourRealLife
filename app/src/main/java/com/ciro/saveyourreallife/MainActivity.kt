package com.ciro.saveyourreallife

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ciro.saveyourreallife.model.InstalledApp
import com.ciro.saveyourreallife.service.AccessibilityServiceConnection
import com.ciro.saveyourreallife.service.AppBlockAccessibilityService
import com.ciro.saveyourreallife.ui.theme.SaveYourRealLifeTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SaveYourRealLifeTheme {
                val serviceConnected by AccessibilityServiceConnection.isConnected
                    .collectAsStateWithLifecycle()
                MainScreen(
                    viewModel = viewModel,
                    serviceConnected = serviceConnected,
                    openAccessibilitySettings = ::openAccessibilityServiceSettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()
    }

    private fun openAccessibilityServiceSettings() {
        val serviceComponent = ComponentName(this, AppBlockAccessibilityService::class.java)
        val detailsIntent = Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS).apply {
            putExtra(Intent.EXTRA_COMPONENT_NAME, serviceComponent)
        }

        try {
            startActivity(detailsIntent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: SecurityException) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private companion object {
        const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS =
            "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    viewModel: MainViewModel,
    serviceConnected: Boolean,
    openAccessibilitySettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    var showAccessibilityExplanation by rememberSaveable { mutableStateOf(false) }
    var showInformation by rememberSaveable { mutableStateOf(false) }

    fun continueToAccessibilitySettings() {
        viewModel.markAccessibilityExplanationSeen()
        showAccessibilityExplanation = false
        openAccessibilitySettings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SaveYourRealLife") },
                actions = {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.semantics {
                                contentDescription = "Apri menu"
                            },
                        ) {
                            Text("⋮", style = MaterialTheme.typography.headlineSmall)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Accessibilità") },
                                onClick = {
                                    menuExpanded = false
                                    if (state.accessibilityExplanationSeen) {
                                        openAccessibilitySettings()
                                    } else {
                                        showAccessibilityExplanation = true
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Informazioni") },
                                onClick = {
                                    menuExpanded = false
                                    showInformation = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ServiceStatus(
                    connected = serviceConnected,
                    onConfigure = {
                        if (state.accessibilityExplanationSeen) {
                            openAccessibilitySettings()
                        } else {
                            showAccessibilityExplanation = true
                        }
                    },
                )
            }

            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::updateQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    label = { Text("Cerca app") },
                    placeholder = { Text("Nome dell’app") },
                )
            }

            item {
                Text(
                    text = "App installate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }

            if (state.loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.apps.isEmpty()) {
                item {
                    Text(
                        text = if (state.query.isBlank()) {
                            "Nessuna app bloccabile trovata."
                        } else {
                            "Nessun risultato per questa ricerca."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                items(state.apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        blocked = app.packageName in state.blockedPackages,
                        onBlockedChange = { blocked ->
                            viewModel.setBlocked(app.packageName, blocked)
                        },
                    )
                }
            }
        }
    }

    if (showAccessibilityExplanation) {
        AccessibilityExplanationDialog(
            onDismiss = { showAccessibilityExplanation = false },
            onOpenSettings = ::continueToAccessibilitySettings,
        )
    }

    if (showInformation) {
        InformationDialog(onDismiss = { showInformation = false })
    }
}

@Composable
private fun ServiceStatus(connected: Boolean, onConfigure: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(top = 4.dp)
            .then(if (connected) Modifier else Modifier.clickable(onClick = onConfigure)),
        shape = RoundedCornerShape(50),
        color = if (connected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
    ) {
        Text(
            text = if (connected) "Blocco attivo" else "Servizio non attivo",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = if (connected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun AccessibilityExplanationDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accessibilità") },
        text = {
            Text(
                "SaveYourRealLife usa il servizio di Accessibilità solo per identificare " +
                    "l’app in primo piano. Se l’app è bloccata, torna alla Home e mostra un " +
                    "breve avviso. Non legge testi, messaggi o contenuti dello schermo e non " +
                    "recupera l’albero delle finestre. Puoi disabilitare il servizio in " +
                    "qualsiasi momento dalle impostazioni di Accessibilità.",
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Apri impostazioni")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        },
    )
}

@Composable
private fun InformationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Informazioni") },
        text = {
            Text(
                "SaveYourRealLife conserva sul dispositivo la lista scelta e usa soltanto " +
                    "il servizio di Accessibilità per rilevare l’app aperta, tornare alla Home " +
                    "e mostrare il popup di blocco. Non usa Internet, account, root o " +
                    "amministrazione del dispositivo e non richiede il permesso di " +
                    "sovrapposizione. Il servizio non legge testi, messaggi o contenuti dello " +
                    "schermo e può essere disabilitato in qualsiasi momento nelle impostazioni " +
                    "di Accessibilità.",
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        },
    )
}

@Composable
private fun AppRow(
    app: InstalledApp,
    blocked: Boolean,
    onBlockedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBlockedChange(!blocked) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            bitmap = app.icon.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = app.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = if (blocked) "Bloccata" else "Consentita",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = blocked,
            onCheckedChange = onBlockedChange,
        )
    }
}
