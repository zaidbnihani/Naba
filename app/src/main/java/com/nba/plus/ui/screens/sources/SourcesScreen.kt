package com.nba.plus.ui.screens.sources

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nba.plus.R
import com.nba.plus.domain.model.Source
import com.nba.plus.domain.repository.SourcesRepository
import com.nba.plus.ui.components.AppTopBar
import com.nba.plus.ui.components.ErrorState
import com.nba.plus.ui.components.SectionHeader
import com.nba.plus.ui.components.SourceListItem
import com.nba.plus.ui.components.FeedSkeleton
import com.nba.plus.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourcesUiState(
    val loading: Boolean = true,
    val errorMessage: Boolean = false,
    val groups: List<Pair<String, List<Source>>> = emptyList(),
)

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val sourcesRepository: SourcesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SourcesUiState())
    val state: StateFlow<SourcesUiState> = _state.asStateFlow()

    val followedIds: StateFlow<Set<String>> = sourcesRepository.observeFollowedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, errorMessage = false) }
        viewModelScope.launch {
            runCatching { sourcesRepository.getSourcesGrouped() }
                .onSuccess { groups ->
                    _state.update {
                        it.copy(loading = false, groups = groups.toList())
                    }
                }
                .onFailure {
                    _state.update { s -> s.copy(loading = false, errorMessage = true) }
                }
        }
    }

    fun toggleFollow(source: Source) {
        viewModelScope.launch {
            val isFollowed = source.id in followedIds.value
            sourcesRepository.setFollowed(source.id, !isFollowed)
        }
    }
}

/**
 * شاشة «مصادري»: شريط علوي (ترس + بحث) وقوائم مصادر مجمّعة حسب الفئة.
 */
@Composable
fun SourcesScreen(
    navController: NavController,
    viewModel: SourcesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val followedIds by viewModel.followedIds.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AppTopBar(
            title = stringResource(R.string.sources_title),
            actions = {
                IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                }
                IconButton(onClick = { navController.navigate(Routes.PROFILE) }) {
                    Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
                }
            },
        )

        when {
            state.loading -> FeedSkeleton(itemCount = 6)

            state.errorMessage -> {
                ErrorState(
                    title = stringResource(R.string.error_generic_title),
                    message = stringResource(R.string.error_generic_message),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = viewModel::load,
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    state.groups.forEach { (categoryId, sources) ->
                        item(key = "header_$categoryId") {
                            SectionHeader(title = categoryDisplayName(categoryId))
                        }
                        items(sources.size, key = { sources[it].id }) { index ->
                            val source = sources[index]
                            SourceListItem(
                                source = source,
                                onClick = { navController.navigate(Routes.source(source.id)) },
                            ) {
                                val followed = source.id in followedIds
                                androidx.compose.material3.Text(
                                    text = if (followed) {
                                        stringResource(R.string.following)
                                    } else {
                                        stringResource(R.string.follow)
                                    },
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                                    color = if (followed) {
                                        androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        androidx.compose.material3.MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** اسم الفئة بالعربية من معرّفها. */
fun categoryDisplayName(categoryId: String): String =
    com.nba.plus.data.repository.DefaultCategories.all
        .firstOrNull { it.id == categoryId }?.nameAr ?: categoryId
