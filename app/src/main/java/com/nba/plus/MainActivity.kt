package com.nba.plus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nba.plus.domain.model.ThemeMode
import com.nba.plus.domain.model.UserPreferences
import com.nba.plus.domain.repository.UserPreferencesRepository
import com.nba.plus.ui.navigation.AppRoot
import com.nba.plus.ui.navigation.Routes
import com.nba.plus.ui.theme.LocalHeadlineScale
import com.nba.plus.ui.theme.NbaTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val preferences = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    /** وجهة البداية: الترحيب عند أول تشغيل أو الرئيسية. */
    val startDestination = preferencesRepository.preferences
        .map { if (it.onboardingCompleted) Routes.HOME else Routes.ONBOARDING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null as String?)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()

            val darkTheme = when (prefs.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            // دعم RTL على مستوى التطبيق كاملًا مع أيقونات معكوسة
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalHeadlineScale provides prefs.headlineFontSize.scale,
            ) {
                NbaTheme(darkTheme = darkTheme) {
                    val start = startDestination
                    if (start == null) {
                        Box(Modifier.fillMaxSize())
                    } else {
                        AppRoot(startDestination = start)
                    }
                }
            }
        }
    }
}
