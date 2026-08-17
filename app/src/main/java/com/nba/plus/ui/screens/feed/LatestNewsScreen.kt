package com.nba.plus.ui.screens.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.nba.plus.domain.model.FeedTab
import com.nba.plus.domain.repository.InteractionsRepository
import com.nba.plus.domain.repository.NewsRepository
import com.nba.plus.domain.repository.SavedArticlesRepository
import com.nba.plus.domain.usecase.GetFeedUseCase
import com.nba.plus.ui.components.AppTopBar
import com.nba.plus.ui.components.EmptyState
import com.nba.plus.ui.components.ErrorState
import com.nba.plus.ui.components.FeedSkeleton
import com.nba.plus.ui.components.LoadMoreIndicator
import com.nba.plus.ui.components.NewsCard
import com.nba.plus.ui.components.NewsCardSkeleton
import com.nba.plus.ui.components.TopTabRow
import com.nba.plus.ui.navigation.Routes
import com.nba.plus.ui.util.shareArticle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getFeed: GetFeedUseCase,
    newsRepository: NewsRepository,
    private val savedArticlesRepository: SavedArticlesRepository,
    private val interactionsRepository: InteractionsRepository,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(FeedTab.LATEST)

    @OptIn(ExperimentalCoroutinesApi::class)
    val articles: Flow<PagingData<Article>> = selectedTab
        .flatMapLatest { tab -> getFeed(tab) }
        .cachedIn(viewModelScope)

    val tab: StateFlow<FeedTab> = selectedTab.asStateFlow()

    val savedIds: StateFlow<Set<String>> = savedArticlesRepository.observeSavedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val likedIds: StateFlow<Set<String>> = interactionsRepository.observeLikedArticleIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** نسخة مخزّنة محليًا تُعرض عند فقد الاتصال. */
    val cachedArticles: StateFlow<List<Article>> = newsRepository.observeCached()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectTab(tab: FeedTab) {
        selectedTab.value = tab
    }

    fun toggleSave(article: Article) {
        viewModelScope.launch { savedArticlesRepository.toggleSaved(article) }
    }

    fun toggleLike(article: Article) {
        viewModelScope.launch { interactionsRepository.toggleLike(article) }
    }
}

/**
 * شاشة «آخر الأخبار»: تبويبات علوية (الأكثر قراءة/آخر الأخبار/عاجل/لك)
 * وتحتها تغذية رأسية ببطاقات الأخبار مع ترقيم وسحب للتحديث.
 */
@Composable
fun LatestNewsScreen(
    navController: NavController,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val tabLabels = FeedTab.entries.map { stringResource(it.labelRes) }
    val selectedTab by viewModel.tab.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedIds.collectAsStateWithLifecycle()
    val cached by viewModel.cachedArticles.collectAsStateWithLifecycle()

    val lazyItems = viewModel.articles.collectAsLazyPagingItems()
    val refreshState = lazyItems.loadState.refresh

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AppTopBar(
            title = stringResource(R.string.tab_latest),
            actions = {
                IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                }
            },
        )

        TopTabRow(
            tabs = tabLabels,
            selectedIndex = selectedTab.ordinal,
            onSelect = { viewModel.selectTab(FeedTab.entries[it]) },
        )

        when {
            refreshState is LoadState.Loading && lazyItems.itemCount == 0 -> {
                FeedSkeleton()
            }

            refreshState is LoadState.Error && lazyItems.itemCount == 0 -> {
                if (cached.isNotEmpty()) {
                    OfflineCachedFeed(
                        articles = cached,
                        savedIds = savedIds,
                        likedIds = likedIds,
                        onArticleClick = { navController.navigate(Routes.article(it.id)) },
                        onToggleSave = viewModel::toggleSave,
                        onToggleLike = viewModel::toggleLike,
                        onShare = { shareArticle(context, it) },
                    )
                } else {
                    ErrorState(
                        title = stringResource(R.string.error_network_title),
                        message = stringResource(R.string.error_network_message),
                        retryLabel = stringResource(R.string.retry),
                        onRetry = { lazyItems.retry() },
                    )
                }
            }

            lazyItems.itemCount == 0 && refreshState is LoadState.NotLoading -> {
                EmptyState(
                    title = stringResource(R.string.empty_category_title),
                    message = stringResource(R.string.empty_category_message),
                )
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = refreshState is LoadState.Loading,
                    onRefresh = { lazyItems.refresh() },
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 6.dp),
                    ) {
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
}

@Composable
private fun OfflineCachedFeed(
    articles: List<Article>,
    savedIds: Set<String>,
    likedIds: Set<String>,
    onArticleClick: (Article) -> Unit,
    onToggleSave: (Article) -> Unit,
    onToggleLike: (Article) -> Unit,
    onShare: (Article) -> Unit,
) {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = stringResource(R.string.offline_banner),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(10.dp),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(articles.size, key = { articles[it].id }) { index ->
                val article = articles[index]
                NewsCard(
                    article = article,
                    isLiked = article.id in likedIds,
                    isSaved = article.id in savedIds,
                    onToggleLike = { onToggleLike(article) },
                    onToggleSave = { onToggleSave(article) },
                    onShare = { onShare(article) },
                    onClick = { onArticleClick(article) },
                )
            }
        }
    }
}
