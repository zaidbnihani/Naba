package com.nba.plus.ui.screens.categories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.nba.plus.domain.repository.CategoriesRepository
import com.nba.plus.domain.repository.InteractionsRepository
import com.nba.plus.domain.repository.NewsRepository
import com.nba.plus.domain.repository.SavedArticlesRepository
import com.nba.plus.ui.components.AppTopBar
import com.nba.plus.ui.components.EmptyState
import com.nba.plus.ui.components.ErrorState
import com.nba.plus.ui.components.FeedSkeleton
import com.nba.plus.ui.components.FollowButton
import com.nba.plus.ui.components.LoadMoreIndicator
import com.nba.plus.ui.components.NewsCard
import com.nba.plus.ui.components.NewsCardSkeleton
import com.nba.plus.ui.navigation.Routes
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

data class CategoryFeedUiState(
    val loading: Boolean = true,
    val categoryName: String = "",
)

@HiltViewModel
class CategoryFeedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    newsRepository: NewsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val savedArticlesRepository: SavedArticlesRepository,
    private val interactionsRepository: InteractionsRepository,
) : ViewModel() {

    val categoryId: String = savedStateHandle.get<String>("categoryId").orEmpty()

    private val _state = MutableStateFlow(CategoryFeedUiState())
    val state: StateFlow<CategoryFeedUiState> = _state.asStateFlow()

    /** تغذية الفئة (مرقّمة). */
    val articles: Flow<PagingData<Article>> = newsRepository
        .pagedFeed(com.nba.plus.domain.repository.FeedSpec(categoryId = categoryId))
        .cachedIn(viewModelScope)

    /** آخر ما خُزّن محليًا لهذه الفئة (عرض دون اتصال). */
    val cachedArticles: Flow<List<Article>> =
        newsRepository.observeCached(
            com.nba.plus.domain.repository.FeedSpec(categoryId = categoryId)
        )

    val isFollowing: StateFlow<Boolean> = categoriesRepository.observeFollowedIds()
        .map { categoryId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val savedIds: StateFlow<Set<String>> = savedArticlesRepository.observeSavedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val likedIds: StateFlow<Set<String>> = interactionsRepository.observeLikedArticleIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = false,
                    categoryName = categoriesRepository.getCategory(categoryId)?.nameAr.orEmpty(),
                )
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            categoriesRepository.setFollowed(categoryId, !isFollowing.value)
        }
    }

    fun toggleSave(article: Article) {
        viewModelScope.launch { savedArticlesRepository.toggleSaved(article) }
    }

    fun toggleLike(article: Article) {
        viewModelScope.launch { interactionsRepository.toggleLike(article) }
    }
}

/** شاشة أخبار فئة واحدة مع زر متابعة الفئة. */
@Composable
fun CategoryFeedScreen(
    navController: NavController,
    viewModel: CategoryFeedViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedIds.collectAsStateWithLifecycle()
    val isFollowing by viewModel.isFollowing.collectAsStateWithLifecycle()

    val lazyItems = viewModel.articles.collectAsLazyPagingItems()
    val refreshState = lazyItems.loadState.refresh

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AppTopBar(
            title = stringResource(R.string.category_articles, state.categoryName),
            showBack = true,
            onBack = { navController.popBackStack() },
            actions = {
                FollowButton(
                    isFollowing = isFollowing,
                    onToggle = viewModel::toggleFollow,
                    modifier = Modifier.padding(end = 8.dp),
                )
            },
        )

        when {
            refreshState is LoadState.Loading && lazyItems.itemCount == 0 -> FeedSkeleton()

            refreshState is LoadState.Error && lazyItems.itemCount == 0 -> {
                ErrorState(
                    title = stringResource(R.string.error_network_title),
                    message = stringResource(R.string.error_network_message),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = { lazyItems.retry() },
                )
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
