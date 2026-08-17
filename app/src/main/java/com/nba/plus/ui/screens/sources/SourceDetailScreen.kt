package com.nba.plus.ui.screens.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.cachedIn
import com.nba.plus.R
import com.nba.plus.domain.model.Article
import com.nba.plus.domain.model.Source
import com.nba.plus.domain.repository.InteractionsRepository
import com.nba.plus.domain.repository.NewsRepository
import com.nba.plus.domain.repository.SavedArticlesRepository
import com.nba.plus.domain.repository.SourcesRepository
import com.nba.plus.domain.repository.UserPreferencesRepository
import com.nba.plus.ui.components.AlertsToggleRow
import com.nba.plus.ui.components.AppTopBar
import com.nba.plus.ui.components.ErrorState
import com.nba.plus.ui.components.FeedSkeleton
import com.nba.plus.ui.components.FollowButton
import com.nba.plus.ui.components.LoadMoreIndicator
import com.nba.plus.ui.components.NewsCard
import com.nba.plus.ui.components.NewsCardSkeleton
import com.nba.plus.ui.components.SectionHeader
import com.nba.plus.ui.components.ShimmerAsyncImage
import com.nba.plus.ui.components.SourceAvatar
import com.nba.plus.ui.navigation.Routes
import com.nba.plus.ui.util.TimeFormat
import com.nba.plus.ui.util.shareArticle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourceDetailUiState(
    val loading: Boolean = true,
    val errorMessage: Boolean = false,
    val source: Source? = null,
)

@HiltViewModel
class SourceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sourcesRepository: SourcesRepository,
    newsRepository: NewsRepository,
    private val savedArticlesRepository: SavedArticlesRepository,
    private val interactionsRepository: InteractionsRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val sourceId: String = savedStateHandle.get<String>("sourceId").orEmpty()

    private val _state = MutableStateFlow(SourceDetailUiState())
    val state: StateFlow<SourceDetailUiState> = _state.asStateFlow()

    /** تغذية مقالات المصدر (مرقّمة). */
    val articles: Flow<PagingData<Article>> = newsRepository
        .pagedFeed(com.nba.plus.domain.repository.FeedSpec(sourceId = sourceId))
        .cachedIn(viewModelScope)

    val isFollowing: StateFlow<Boolean> = sourcesRepository.observeFollowedIds()
        .map { sourceId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val breakingAlerts: StateFlow<Boolean> = preferencesRepository.preferences
        .map { sourceId in it.alertSourceIds }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val savedIds: StateFlow<Set<String>> = savedArticlesRepository.observeSavedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val likedIds: StateFlow<Set<String>> = interactionsRepository.observeLikedArticleIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch {
            runCatching { sourcesRepository.getSource(sourceId) }
                .onSuccess { source ->
                    _state.update {
                        it.copy(loading = false, source = source)
                    }
                }
                .onFailure {
                    _state.update { s -> s.copy(loading = false, errorMessage = true) }
                }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            sourcesRepository.setFollowed(sourceId, !isFollowing.value)
        }
    }

    fun toggleBreakingAlerts() {
        viewModelScope.launch {
            preferencesRepository.update { prefs ->
                val current = prefs.alertSourceIds
                prefs.copy(
                    alertSourceIds = if (sourceId in current) current - sourceId
                    else current + sourceId
                )
            }
        }
    }

    fun toggleSave(article: Article) {
        viewModelScope.launch { savedArticlesRepository.toggleSaved(article) }
    }

    fun toggleLike(article: Article) {
        viewModelScope.launch { interactionsRepository.toggleLike(article) }
    }
}

/**
 * شاشة تفاصيل المصدر: بانر + شعار بإطار + وصف + متابعون + زر متابعة
 * + مفتاح تنبيهات العاجل + تغذية مقالات المصدر.
 */
@Composable
fun SourceDetailScreen(
    navController: NavController,
    viewModel: SourceDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isFollowing by viewModel.isFollowing.collectAsStateWithLifecycle()
    val breakingAlerts by viewModel.breakingAlerts.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedIds.collectAsStateWithLifecycle()

    val lazyItems = viewModel.articles.collectAsLazyPagingItems()
    val refreshState = lazyItems.loadState.refresh

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AppTopBar(
            title = state.source?.name.orEmpty(),
            showBack = true,
            onBack = { navController.popBackStack() },
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // البانر والشعار
            item(key = "banner") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        ShimmerAsyncImage(
                            model = state.source?.bannerUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                        )
                    }
                    SourceAvatar(
                        name = state.source?.name.orEmpty(),
                        iconUrl = state.source?.iconUrl,
                        size = 84.dp,
                    )
                }
            }

            // الاسم والوصف والمتابعون
            item(key = "source_info") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = state.source?.name.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    if (!state.source?.description.isNullOrBlank()) {
                        Text(
                            text = state.source?.description.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    state.source?.let { source ->
                        Text(
                            text = stringResource(
                                R.string.followers_count,
                                TimeFormat.compact(source.followersCount),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FollowButton(
                        isFollowing = isFollowing,
                        onToggle = viewModel::toggleFollow,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
            }

            item(key = "alerts") {
                AlertsToggleRow(
                    title = stringResource(R.string.source_breaking_alerts),
                    checked = breakingAlerts,
                    onCheckedChange = { viewModel.toggleBreakingAlerts() },
                )
            }

            item(key = "divider") {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            item(key = "articles_header") {
                SectionHeader(title = stringResource(R.string.source_articles))
            }

            when {
                refreshState is LoadState.Loading && lazyItems.itemCount == 0 -> {
                    item(key = "loading") { FeedSkeleton(itemCount = 3) }
                }

                refreshState is LoadState.Error && lazyItems.itemCount == 0 -> {
                    item(key = "error") {
                        ErrorState(
                            title = stringResource(R.string.error_network_title),
                            message = stringResource(R.string.error_network_message),
                            retryLabel = stringResource(R.string.retry),
                            onRetry = { lazyItems.retry() },
                        )
                    }
                }

                else -> {
                    items(
                        count = lazyItems.itemCount,
                        key = lazyItems.itemKey { it.id },
                    ) { index ->
                        val article = lazyItems[index]
                        if (article == null) {
                            NewsCardSkeleton()
                        } else {
                            NewsCard(
                                article = article,
                                isLiked = article.id in likedIds,
                                isSaved = article.id in savedIds,
                                onToggleLike = { viewModel.toggleLike(article) },
                                onToggleSave = { viewModel.toggleSave(article) },
                                onShare = { shareArticle(context, article) },
                                onClick = { navController.navigate(Routes.article(article.id)) },
                            )
                        }
                    }
                    if (lazyItems.loadState.append is LoadState.Loading) {
                        item(key = "load_more") { LoadMoreIndicator() }
                    }
                }
            }
        }
    }
}
