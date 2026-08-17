package com.nba.plus.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nba.plus.R
import com.nba.plus.domain.model.NewsCategory
import com.nba.plus.domain.model.Source
import com.nba.plus.domain.repository.CategoriesRepository
import com.nba.plus.domain.repository.SourcesRepository
import com.nba.plus.domain.repository.UserPreferencesRepository
import com.nba.plus.ui.components.SourceAvatar
import com.nba.plus.ui.theme.Purple
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val step: Int = 0,
    val loading: Boolean = true,
    val categories: List<NewsCategory> = emptyList(),
    val selectedCategories: Set<String> = emptySet(),
    val sources: List<Source> = emptyList(),
    val selectedSources: Set<String> = emptySet(),
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val categoriesRepository: CategoriesRepository,
    private val sourcesRepository: SourcesRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val sources = runCatching {
                sourcesRepository.getSourcesGrouped().values.flatten()
            }.getOrDefault(emptyList())
            _state.update {
                it.copy(
                    loading = false,
                    categories = categoriesRepository.getCategories(),
                    sources = sources,
                )
            }
        }
    }

    fun toggleCategory(id: String) {
        _state.update { s ->
            s.copy(
                selectedCategories = if (id in s.selectedCategories) {
                    s.selectedCategories - id
                } else {
                    s.selectedCategories + id
                }
            )
        }
    }

    fun toggleSource(id: String) {
        _state.update { s ->
            s.copy(
                selectedSources = if (id in s.selectedSources) {
                    s.selectedSources - id
                } else {
                    s.selectedSources + id
                }
            )
        }
    }

    fun next() {
        if (_state.value.selectedCategories.size >= MIN_CATEGORIES) {
            _state.update { it.copy(step = 1) }
        }
    }

    fun back() {
        _state.update { it.copy(step = 0) }
    }

    /** يخزّن الاختيارات ويُكمل الترحيب ثم يستدعي التنقل. */
    fun finish(onFinished: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            categoriesRepository.setFollowedAll(s.selectedCategories)
            s.selectedSources.forEach { sourcesRepository.setFollowed(it, true) }
            preferencesRepository.update { it.copy(onboardingCompleted = true) }
            onFinished()
        }
    }

    companion object {
        const val MIN_CATEGORIES = 3
    }
}

/**
 * شاشة الترحيب (أول تشغيل): اختيار المجالات ثم المصادر قبل الدخول.
 */
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.step == 1) {
                IconButton(onClick = viewModel::back) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    viewModel.finish {
                        navController.navigate(com.nba.plus.ui.navigation.Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
            ) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = stringResource(
                    if (state.step == 0) {
                        R.string.onboarding_categories_title
                    } else {
                        R.string.onboarding_sources_title
                    }
                ),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    if (state.step == 0) {
                        R.string.onboarding_categories_subtitle
                    } else {
                        R.string.onboarding_sources_subtitle
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
        }

        Box(Modifier.fillMaxSize()) {
            if (state.step == 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    items(state.categories, key = { it.id }) { category ->
                        val selected = category.id in state.selectedCategories
                        Column(
                            modifier = Modifier
                                .background(
                                    if (selected) Purple else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(14.dp),
                                )
                                .border(
                                    width = if (selected) 0.dp else 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .clickable { viewModel.toggleCategory(category.id) }
                                .padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = category.nameAr,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp,
                    ),
                ) {
                    items(state.sources.size, key = { state.sources[it].id }) { index ->
                        val source = state.sources[index]
                        val selected = source.id in state.selectedSources
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(14.dp),
                                )
                                .clickable { viewModel.toggleSource(source.id) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SourceAvatar(name = source.name, iconUrl = source.iconUrl, size = 36.dp)
                            Text(
                                text = source.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Purple,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // زر المتابعة السفلي
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
            ) {
                if (state.step == 0 && state.selectedCategories.size < OnboardingViewModel.MIN_CATEGORIES) {
                    Text(
                        text = stringResource(R.string.onboarding_min_categories),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                Button(
                    onClick = {
                        if (state.step == 0) {
                            viewModel.next()
                        } else {
                            viewModel.finish {
                                navController.navigate(com.nba.plus.ui.navigation.Routes.HOME) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    },
                    enabled = state.step == 1 ||
                        state.selectedCategories.size >= OnboardingViewModel.MIN_CATEGORIES,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (state.step == 0) {
                                R.string.onboarding_next
                            } else {
                                R.string.onboarding_finish
                            }
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
