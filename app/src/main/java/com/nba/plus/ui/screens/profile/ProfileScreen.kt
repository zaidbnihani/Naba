package com.nba.plus.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nba.plus.BuildConfig
import com.nba.plus.R
import com.nba.plus.data.remote.ApiConfig
import com.nba.plus.domain.model.AuthState
import com.nba.plus.domain.model.HeadlineFontSize
import com.nba.plus.domain.model.Source
import com.nba.plus.domain.model.ThemeMode
import com.nba.plus.domain.model.UserPreferences
import com.nba.plus.domain.repository.AuthRepository
import com.nba.plus.domain.repository.PersonalizationRepository
import com.nba.plus.domain.repository.SourcesRepository
import com.nba.plus.domain.repository.UserPreferencesRepository
import com.nba.plus.ui.components.AppSwitch
import com.nba.plus.ui.components.SettingRow
import com.nba.plus.ui.components.SettingsSectionCard
import com.nba.plus.ui.components.VersionFooter
import com.nba.plus.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val personalizationRepository: PersonalizationRepository,
    private val authRepository: AuthRepository,
    private val sourcesRepository: SourcesRepository,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _alertSources = MutableStateFlow<List<Source>>(emptyList())
    val alertSources: StateFlow<List<Source>> = _alertSources.asStateFlow()

    val versionName: String = BuildConfig.VERSION_NAME
    val demoMode: Boolean = ApiConfig.isDemoMode

    init {
        viewModelScope.launch {
            runCatching {
                sourcesRepository.getSourcesGrouped().values.flatten()
            }.onSuccess { _alertSources.value = it }
        }
    }

    fun setThemeMode(mode: ThemeMode) = updatePrefs { it.copy(themeMode = mode) }

    fun setHeadlineFontSize(size: HeadlineFontSize) = updatePrefs { it.copy(headlineFontSize = size) }

    fun setAutoplay(enabled: Boolean) = updatePrefs { it.copy(autoplayVideos = enabled) }

    fun setBreakingAlerts(enabled: Boolean) = updatePrefs { it.copy(breakingAlerts = enabled) }

    fun setTopStoriesAlerts(enabled: Boolean) = updatePrefs { it.copy(topStoriesAlerts = enabled) }

    fun setSportsAlerts(enabled: Boolean) = updatePrefs { it.copy(sportsAlerts = enabled) }

    fun setMatchResultsAlerts(enabled: Boolean) = updatePrefs { it.copy(matchResultsAlerts = enabled) }

    fun setAlertSound(enabled: Boolean) = updatePrefs { it.copy(alertSound = enabled) }

    fun setPersonalization(enabled: Boolean) {
        viewModelScope.launch { personalizationRepository.setEnabled(enabled) }
    }

    fun toggleAlertSource(sourceId: String) = updatePrefs { prefs ->
        val current = prefs.alertSourceIds
        prefs.copy(
            alertSourceIds = if (sourceId in current) current - sourceId else current + sourceId
        )
    }

    fun logout() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun deleteAccount() {
        viewModelScope.launch { authRepository.deleteAccount() }
    }

    private fun updatePrefs(transform: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch { preferencesRepository.update(transform) }
    }
}

/**
 * شاشة «حسابي»: ترويسة المستخدم + روابط سريعة + أقسام الإعدادات
 * والتنبيهات والذكاء الاصطناعي + سياسة الخصوصية/خروج/حذف/إصدار.
 */
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val alertSources by viewModel.alertSources.collectAsStateWithLifecycle()

    var showFontDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAlertSourcesDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // ===== ترويسة الحساب =====
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val displayName = authState.displayName?.takeIf { it.isNotBlank() }
                val displayEmail = authState.email?.takeIf { it.isNotBlank() }
                val initialLetter = (displayName ?: displayEmail ?: stringResource(R.string.profile_guest)).take(1).uppercase()

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (authState.isSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initialLetter,
                        color = if (authState.isSignedIn) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = displayName ?: displayEmail ?: stringResource(R.string.profile_guest),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (authState.isSignedIn) {
                            displayEmail ?: ""
                        } else {
                            stringResource(R.string.profile_guest_subtitle)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.then(
                            if (!authState.isSignedIn) Modifier.clickable { navController.navigate(Routes.LOGIN) } else Modifier
                        ),
                    )
                }
                if (!authState.isSignedIn) {
                    Button(
                        onClick = { navController.navigate(Routes.LOGIN) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.login_button))
                    }
                }
            }
        }

        // ===== روابط سريعة (زران متجاوران كما في اللقطة) =====
        item(key = "quick_links") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickLinkCard(
                    icon = Icons.Filled.Bookmark,
                    label = stringResource(R.string.profile_favorites),
                    modifier = Modifier.weight(1f),
                ) { navController.navigate(Routes.SAVED) }
                QuickLinkCard(
                    icon = Icons.Filled.ThumbUp,
                    label = stringResource(R.string.profile_interactions),
                    modifier = Modifier.weight(1f),
                ) { navController.navigate(Routes.INTERACTIONS) }
            }
        }

        // ===== الإعدادات =====
        item(key = "settings_header") {
            SectionTitle(stringResource(R.string.section_settings))
        }
        item(key = "settings") {
            SettingsSectionCard {
                SettingRow(
                    title = stringResource(R.string.settings_edit_sources),
                    icon = Icons.Filled.Source,
                    onClick = { navController.navigate(Routes.SOURCES) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow(
                    title = stringResource(R.string.settings_headline_font_size),
                    icon = Icons.Filled.FormatSize,
                    value = stringResource(prefs.headlineFontSize.labelRes),
                    onClick = { showFontDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow(
                    title = stringResource(R.string.settings_autoplay),
                    icon = Icons.Outlined.PlayCircle,
                    trailingSwitch = {
                        AppSwitch(
                            checked = prefs.autoplayVideos,
                            onCheckedChange = viewModel::setAutoplay,
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow(
                    title = stringResource(R.string.settings_dark_mode),
                    icon = Icons.Outlined.DarkMode,
                    value = stringResource(prefs.themeMode.labelRes),
                    onClick = { showThemeDialog = true },
                )
            }
        }

        // ===== التنبيهات =====
        item(key = "notifications_header") {
            SectionTitle(stringResource(R.string.section_notifications))
        }
        item(key = "notifications") {
            SettingsSectionCard {
                SettingRow(
                    title = stringResource(R.string.notif_breaking),
                    icon = Icons.Outlined.NotificationsActive,
                    trailingSwitch = {
                        AppSwitch(
                            checked = prefs.breakingAlerts,
                            onCheckedChange = { enabled ->
                                if (enabled &&
                                    android.os.Build.VERSION.SDK_INT >= 33
                                ) {
                                    permissionLauncher.launch(
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    )
                                }
                                viewModel.setBreakingAlerts(enabled)
                            },
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow(
                    title = stringResource(R.string.notif_choose_sources),
                    icon = Icons.Filled.Source,
                    onClick = { showAlertSourcesDialog = true },
                    value = prefs.alertSourceIds.size.toString(),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow(
                    title = stringResource(R.string.notif_top),
                    icon = Icons.Outlined.NotificationsActive,
                    trailingSwitch = {
                        AppSwitch(
                            checked = prefs.topStoriesAlerts,
                            onCheckedChange = viewModel::setTopStoriesAlerts,
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow(
                    title = stringResource(R.string.notif_sports),
                    icon = Icons.Outlined.NotificationsActive,
                    trailingSwitch = {
                        AppSwitch(
                            checked = prefs.sportsAlerts,
                            onCheckedChange = viewModel::setSportsAlerts,
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow(
                    title = stringResource(R.string.notif_matches),
                    icon = Icons.Outlined.NotificationsActive,
                    trailingSwitch = {
                        AppSwitch(
                            checked = prefs.matchResultsAlerts,
                            onCheckedChange = viewModel::setMatchResultsAlerts,
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow(
                    title = stringResource(R.string.notif_sound),
                    icon = Icons.Outlined.NotificationsActive,
                    trailingSwitch = {
                        AppSwitch(
                            checked = prefs.alertSound,
                            onCheckedChange = viewModel::setAlertSound,
                        )
                    },
                )
            }
        }

        // ===== الذكاء الاصطناعي =====
        item(key = "ai_header") {
            SectionTitle(stringResource(R.string.section_ai))
        }
        item(key = "ai") {
            SettingsSectionCard {
                SettingRow(
                    title = stringResource(R.string.ai_personalization),
                    subtitle = stringResource(R.string.ai_personalization_desc),
                    icon = Icons.Filled.SmartToy,
                    trailingSwitch = {
                        AppSwitch(
                            checked = prefs.aiPersonalizationEnabled,
                            onCheckedChange = viewModel::setPersonalization,
                        )
                    },
                )
            }
        }

        // ===== إضافات =====
        item(key = "extras") {
            SettingsSectionCard {
                SettingRow(
                    title = stringResource(R.string.privacy_policy),
                    icon = Icons.Filled.Policy,
                    onClick = {
                        runCatching {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(PRIVACY_POLICY_URL),
                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow(
                    title = stringResource(R.string.demo_mode),
                    icon = Icons.Filled.SmartToy,
                    value = if (viewModel.demoMode) {
                        stringResource(R.string.active)
                    } else {
                        stringResource(R.string.inactive)
                    },
                )
            }
        }

        // ===== خروج / حذف أو تسجيل الدخول =====
        item(key = "danger_zone") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (authState.isSignedIn) {
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp).size(18.dp),
                        )
                        Text(stringResource(R.string.logout))
                    }
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                        )
                        Text(stringResource(R.string.delete_account))
                    }
                } else {
                    Button(
                        onClick = { navController.navigate(Routes.LOGIN) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.login_button),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item(key = "version") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                VersionFooter(
                    versionLabel = stringResource(R.string.version),
                    versionValue = viewModel.versionName,
                )
            }
        }
    }

    // ===== الحوارات =====

    if (showFontDialog) {
        OptionsDialog(
            title = stringResource(R.string.settings_headline_font_size),
            options = HeadlineFontSize.entries.map { stringResource(it.labelRes) },
            selectedIndex = HeadlineFontSize.entries.indexOf(prefs.headlineFontSize),
            onSelect = { viewModel.setHeadlineFontSize(HeadlineFontSize.entries[it]) },
            onDismiss = { showFontDialog = false },
        )
    }

    if (showThemeDialog) {
        OptionsDialog(
            title = stringResource(R.string.settings_dark_mode),
            options = ThemeMode.entries.map { stringResource(it.labelRes) },
            selectedIndex = ThemeMode.entries.indexOf(prefs.themeMode),
            onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showLogoutDialog) {
        ConfirmDialog(
            title = stringResource(R.string.logout_confirm_title),
            message = stringResource(R.string.logout_confirm_message),
            confirmLabel = stringResource(R.string.logout),
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.delete_account_confirm_title),
            message = stringResource(R.string.delete_account_confirm_message),
            confirmLabel = stringResource(R.string.delete_account),
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteAccount()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    if (showAlertSourcesDialog) {
        AlertDialog(
            onDismissRequest = { showAlertSourcesDialog = false },
            title = { Text(stringResource(R.string.notif_choose_sources)) },
            text = {
                if (alertSources.isEmpty()) {
                    Text(stringResource(R.string.empty_sources_message))
                } else {
                    LazyColumn {
                        items(alertSources.size, key = { alertSources[it].id }) { index ->
                            val source = alertSources[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleAlertSource(source.id) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = source.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                Checkbox(
                                    checked = source.id in prefs.alertSourceIds,
                                    onCheckedChange = { viewModel.toggleAlertSource(source.id) },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlertSourcesDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            },
        )
    }
}

@Composable
private fun QuickLinkCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
private fun OptionsDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(index)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = index == selectedIndex, onClick = null)
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** TODO: استبدل برابط سياسة الخصوصية الفعلي عند نشر التطبيق. */
private const val PRIVACY_POLICY_URL = "https://example.com/nba-plus/privacy"
