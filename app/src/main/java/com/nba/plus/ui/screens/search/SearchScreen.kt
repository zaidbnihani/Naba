package com.nba.plus.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nba.plus.R
import com.nba.plus.domain.model.Article
import com.nba.plus.domain.repository.NewsRepository
import com.nba.plus.domain.usecase.SearchNewsUseCase
import com.nba.plus.ui.components.CompactNewsCard
import com.nba.plus.ui.components.EmptyState
import com.nba.plus.ui.components.ErrorState
import com.nba.plus.ui.components.FeedSkeleton
import com.nba.plus.ui.components.SectionHeader
import com.nba.plus.ui.components.TrendingChips
import com.nba.plus.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val searching: Boolean = false,
    val errorMessage: Boolean = false,
    val results: List<Article> = emptyList(),
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchNews: SearchNewsUseCase,
    private val newsRepository: NewsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val recentSearches: StateFlow<List<String>> = newsRepository.observeRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _trending = MutableStateFlow<List<String>>(emptyList())
    val trending: StateFlow<List<String>> = _trending.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _trending.value = runCatching {
                newsRepository.getTrendingTopics()
            }.getOrDefault(emptyList())
        }
    }

    fun newSearch(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _state.update {
                it.copy(searching = false, errorMessage = false, results = emptyList())
            }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // مهلة قصيرة قبل إطلاق البحث (debounce)
            _state.update { it.copy(searching = true, errorMessage = false) }
            runCatching { searchNews(query) }
                .onSuccess { results ->
                    _state.update {
                        it.copy(searching = false, results = results)
                    }
                }
                .onFailure {
                    _state.update { s ->
                        s.copy(searching = false, errorMessage = true)
                    }
                }
        }
    }

    fun clearRecents() {
        viewModelScope.launch { newsRepository.clearRecentSearches() }
    }
}

/**
 * شاشة البحث: شريط بحث، بحثات حديثة، رقائق رائجة، نتائج، حالة فراغ.
 */
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recents by viewModel.recentSearches.collectAsStateWithLifecycle()
    val trending by viewModel.trending.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::newSearch,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.newSearch("") }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            )
        }

        when {
            state.query.isBlank() -> {
                RecentAndTrending(
                    recents = recents,
                    trending = trending,
                    onSelect = viewModel::newSearch,
                    onClearRecents = viewModel::clearRecents,
                )
            }

            state.searching -> {
                FeedSkeleton(itemCount = 5)
            }

            state.errorMessage -> {
                ErrorState(
                    title = stringResource(R.string.error_network_title),
                    message = stringResource(R.string.error_network_message),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = { viewModel.newSearch(state.query) },
                )
            }

            state.results.isEmpty() -> {
                EmptyState(
                    title = stringResource(R.string.empty_search_title),
                    message = stringResource(R.string.empty_search_message),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(state.results.size, key = { state.results[it].id }) { index ->
                        val article = state.results[index]
                        CompactNewsCard(
                            article = article,
                            onClick = { navController.navigate(Routes.article(article.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentAndTrending(
    recents: List<String>,
    trending: List<String>,
    onSelect: (String) -> Unit,
    onClearRecents: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        if (recents.isNotEmpty()) {
            item(key = "recents_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.recent_searches),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClearRecents) {
                        Text(stringResource(R.string.clear_all))
                    }
                }
            }
            items(recents.size, key = { "recent_${recents[it]}" }) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(recents[index]) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = recents[index],
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        if (trending.isNotEmpty()) {
            item(key = "trending_header") {
                SectionHeader(title = stringResource(R.string.trending))
            }
            item(key = "trending_chips") {
                TrendingChips(
                    topics = trending,
                    onSelect = onSelect,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        item(key = "spacer") { Spacer(Modifier.height(24.dp)) }
    }
}
