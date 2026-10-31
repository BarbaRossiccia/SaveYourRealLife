package com.ciro.saveyourreallife

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
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
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val serviceConnected by AccessibilityServiceConnection.isConnected
                .collectAsStateWithLifecycle()

            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !state.darkTheme
                    isAppearanceLightNavigationBars = !state.darkTheme
                }
            }
            SaveYourRealLifeTheme(darkTheme = state.darkTheme) {
                MainScreen(
                    state = state,
                    viewModel = viewModel,
                    serviceConnected = serviceConnected,
                    openAppDetails = ::openAppDetails,
                    openAccessibilitySettings = ::openAccessibilityServiceSettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()
    }

    private fun openAppDetails() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName"),
            ),
        )
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
    state: MainUiState,
    viewModel: MainViewModel,
    serviceConnected: Boolean,
    openAppDetails: () -> Unit,
    openAccessibilitySettings: () -> Unit,
) {
    val copy = UiStrings(state.language)
    var menuExpanded by remember { mutableStateOf(false) }
    var showActivation by rememberSaveable { mutableStateOf(false) }
    var showLanguage by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SaveYourRealLife") },
                actions = {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.semantics {
                                contentDescription = copy.openMenu
                            },
                        ) {
                            Text("⋮", style = MaterialTheme.typography.headlineSmall)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(copy.languageMenu) },
                                onClick = {
                                    menuExpanded = false
                                    showLanguage = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(copy.settings) },
                                onClick = {
                                    menuExpanded = false
                                    showSettings = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("About") },
                                onClick = {
                                    menuExpanded = false
                                    showAbout = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ServiceStatus(
                    connected = serviceConnected,
                    copy = copy,
                    onConfigure = { showActivation = true },
                )
            }
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::updateQuery,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    singleLine = true,
                    label = { Text(copy.searchApps) },
                    placeholder = { Text(copy.appName) },
                )
            }
            item {
                Text(
                    text = copy.installedApps,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }
            if (state.loading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(96.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
            } else if (state.apps.isEmpty()) {
                item {
                    Text(
                        text = if (state.query.isBlank()) copy.noApps else copy.noResults,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                items(state.apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        blocked = app.packageName in state.blockedPackages,
                        copy = copy,
                        onBlockedChange = { viewModel.setBlocked(app.packageName, it) },
                    )
                }
            }
        }
    }

    if (showActivation) {
        ActivationGuideDialog(
            copy = copy,
            onDismiss = { showActivation = false },
            onOpenAppDetails = openAppDetails,
            onOpenAccessibility = {
                viewModel.markAccessibilityExplanationSeen()
                showActivation = false
                openAccessibilitySettings()
            },
        )
    }
    if (showLanguage) {
        LanguageDialog(
            selected = state.language,
            copy = copy,
            onSelect = viewModel::setLanguage,
            onDismiss = { showLanguage = false },
        )
    }
    if (showSettings) {
        SettingsDialog(
            darkTheme = state.darkTheme,
            serviceConnected = serviceConnected,
            copy = copy,
            onDarkThemeChange = viewModel::setDarkTheme,
            onDisableService = AccessibilityServiceConnection::disableService,
            onActivateService = {
                showSettings = false
                showActivation = true
            },
            onDismiss = { showSettings = false },
        )
    }
    if (showAbout) AboutDialog(copy = copy, onDismiss = { showAbout = false })
}

@Composable
private fun ServiceStatus(connected: Boolean, copy: UiStrings, onConfigure: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .then(if (connected) Modifier else Modifier.clickable(onClick = onConfigure)),
        shape = RoundedCornerShape(16.dp),
        color = if (connected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = if (connected) copy.blockingActive else copy.activationRequired,
                color = if (connected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (connected) copy.activeDescription else copy.activationDescription,
                modifier = Modifier.padding(top = 3.dp),
                color = if (connected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ActivationGuideDialog(
    copy: UiStrings,
    onDismiss: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onOpenAccessibility: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(copy.activateTitle) },
        text = { Text(copy.activateInstructions) },
        confirmButton = {
            TextButton(onClick = onOpenAccessibility) { Text(copy.openAccessibility) }
        },
        dismissButton = {
            TextButton(onClick = onOpenAppDetails) { Text(copy.openAppInfo) }
        },
    )
}

@Composable
private fun LanguageDialog(
    selected: AppLanguage,
    copy: UiStrings,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(copy.chooseLanguage) },
        text = {
            Column {
                LanguageRow("Italiano", selected == AppLanguage.ITALIAN) {
                    onSelect(AppLanguage.ITALIAN)
                }
                LanguageRow("English", selected == AppLanguage.ENGLISH) {
                    onSelect(AppLanguage.ENGLISH)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(copy.close) } },
    )
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SettingsDialog(
    darkTheme: Boolean,
    serviceConnected: Boolean,
    copy: UiStrings,
    onDarkThemeChange: (Boolean) -> Unit,
    onDisableService: () -> Unit,
    onActivateService: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(copy.settings) },
        text = {
            Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    onDarkThemeChange(!darkTheme)
                }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(copy.darkTheme, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        copy.darkThemeDescription,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = darkTheme, onCheckedChange = onDarkThemeChange)
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    if (serviceConnected) onDisableService() else onActivateService()
                }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(copy.blockingSwitch, style = MaterialTheme.typography.bodyLarge)
                    Text(copy.blockingSwitchDescription, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = serviceConnected, onCheckedChange = {
                    if (it) onActivateService() else onDisableService()
                })
            }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(copy.close) } },
    )
}

@Composable
private fun AboutDialog(copy: UiStrings, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About") },
        text = { Text(copy.aboutText) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(copy.close) } },
    )
}

@Composable
private fun AppRow(
    app: InstalledApp,
    blocked: Boolean,
    copy: UiStrings,
    onBlockedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onBlockedChange(!blocked) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            bitmap = app.icon.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = app.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = if (blocked) copy.blocked else copy.allowed,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(checked = blocked, onCheckedChange = onBlockedChange)
    }
}

private class UiStrings(language: AppLanguage) {
    private val italian = language == AppLanguage.ITALIAN
    private fun t(it: String, en: String) = if (italian) it else en

    val openMenu = t("Apri menu", "Open menu")
    val languageMenu = t("Lingua / Language", "Language / Lingua")
    val settings = t("Impostazioni", "Settings")
    val searchApps = t("Cerca app", "Search apps")
    val appName = t("Nome dell’app", "App name")
    val installedApps = t("App installate", "Installed apps")
    val noApps = t("Nessuna app bloccabile trovata.", "No blockable apps found.")
    val noResults = t("Nessun risultato per questa ricerca.", "No results for this search.")
    val blocked = t("Bloccata", "Blocked")
    val allowed = t("Consentita", "Allowed")
    val blockingActive = t("Blocco attivo", "Blocking active")
    val activeDescription = t(
        "SaveYourRealLife \"sta bloccando\" le app selezionate.",
        "SaveYourRealLife is \"blocking\" the selected apps.",
    )
    val activationRequired = t("Attivazione necessaria", "Activation required")
    val activationDescription = t(
        "Tocca qui per completare l’attivazione dell’app.",
        "Tap here to finish activating the app.",
    )
    val activateTitle = t("Attiva SaveYourRealLife", "Activate SaveYourRealLife")
    val activateInstructions = t(
        "Tocca “Apri Accessibilità” e attiva SaveYourRealLife.\n\nSe Android blocca l’attivazione per impostazioni con limitazioni, torna qui e tocca “Informazioni app”. Tocca i tre puntini (⋮), scegli “Consenti impostazioni con limitazioni” e conferma la tua identità se richiesto. Torna qui e apri di nuovo Accessibilità. Le voci possono variare secondo il dispositivo.\n\nIl servizio identifica l’app in primo piano per bloccare quelle selezionate. Non legge testi, messaggi o contenuti dello schermo.",
        "Tap “Open Accessibility” and enable SaveYourRealLife.\n\nIf Android blocks activation because of restricted settings, return here and tap “App info”. Tap the three dots (⋮), select “Allow restricted settings” and confirm your identity if requested. Return here and open Accessibility again. Labels can vary by device.\n\nThe service identifies the foreground app to block selected apps. It does not read text, messages, or screen content.",
    )
    val openAppInfo = t("Informazioni app", "App info")
    val openAccessibility = t("Apri Accessibilità", "Open Accessibility")
    val close = t("Chiudi", "Close")
    val chooseLanguage = t("Scegli la lingua", "Choose language")
    val darkTheme = t("Tema scuro", "Dark theme")
    val blockingSwitch = t("Blocco app", "App blocking")
    val blockingSwitchDescription = t(
        "Disattiva il servizio di Accessibilità. Per riattivarlo segui la guida.",
        "Turn off the Accessibility service. Follow the guide to enable it again.",
    )
    val darkThemeDescription = t(
        "Passa dall’interfaccia chiara a quella scura.",
        "Switch between the light and dark interface.",
    )
    val aboutText = t(
        "SaveYourRealLife conserva sul dispositivo l’elenco delle app selezionate. Quando il servizio di Accessibilità è attivo, rileva l’app in primo piano e, se è nell’elenco, torna alla Home e mostra un breve avviso. Non legge testi, messaggi o contenuti dello schermo.\n\nIn Impostazioni puoi disattivare il servizio. Per riattivarlo, segui la guida e abilitalo nelle impostazioni di Accessibilità di Android. Puoi disattivarlo anche dalle impostazioni Android. Lingua e tema sono modificabili dal menu.",
        "SaveYourRealLife stores your selected app list on your device. When the Accessibility service is active, it detects the foreground app and, if it is on the list, returns to the Home screen and shows a brief notice. It does not read text, messages, or screen content.\n\nIn Settings you can turn off the service. To enable it again, follow the guide and enable it in Android Accessibility settings. You can also disable it through Android settings. Language and theme can be changed from the menu.",
    )
}
