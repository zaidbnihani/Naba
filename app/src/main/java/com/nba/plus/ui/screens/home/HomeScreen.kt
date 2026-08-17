package com.nba.plus.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nba.plus.R
import com.nba.plus.domain.model.Article
import com.nba.plus.domain.model.NewsCategory
import com.nba.plus.domain.repository.CategoriesRepository
import com.nba.plus.domain.repository.FeedSpec
import com.nba.plus.domain.repository.InteractionsRepository
import com.nba.plus.domain.repository.NewsRepository
import com.nba.plus.domain.repository.SavedArticlesRepository
import com.nba.plus.ui.components.AppTopBar
import com.nba.plus.ui.components.CategoryChipsRow
import com.nba.plus.ui.components.EmptyState
import com.nba.plus.ui.components.ErrorState
import com.nba.plus.ui.components.FeedSkeleton
import com.nba.plus.ui.components.HeroNewsCard
import com.nba.plus.ui.components.NewsCard
import com.nba.plus.ui.components.SectionHeader
import com.nba.plus.ui.navigation.Routes
import com.nba.plus.ui.util.shareArticle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val errorMessage: Boolean = false,
    val hero: Article? = null,
    val articles: List<Article> = emptyList(),
    val selectedCategory: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    categoriesRepository: CategoriesRepository,
    private val savedArticlesRepository: SavedArticlesRepository,
    private val interactionsRepository: InteractionsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val categories: List<NewsCategory> = categoriesRepository.getCategories()

    val savedIds = savedArticlesRepository.observeSavedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val likedIds = interactionsRepository.observeLikedArticleIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private var loadJob: Job? = null

    init {
        reload()
    }

    fun selectCategory(categoryId: String?) {
        _state.update { it.copy(selectedCategory = categoryId) }
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        _state.update { it.copy(loading = true, errorMessage = false) }
        val spec = FeedSpec(
            tab = com.nba.plus.domain.model.FeedTab.LATEST,
            categoryId = _state.value.selectedCategory,
        )
        loadJob = viewModelScope.launch {
            runCatching { newsRepository.fetchPage(spec, page = 1) }
                .onSuccess { page ->
                    val hero = page.firstOrNull { !it.imageUrl.isNullOrBlank() }
                    _state.update {
                        it.copy(
                            loading = false,
                            hero = hero,
                            articles = page.filter { a -> a.id != hero?.id },
                        )
                    }
                }
                .onFailure {
                    _state.update { s -> s.copy(loading = false, errorMessage = true) }
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
 * الشاشة الرئيسية: خبر رئيسي كبير + رقائق فئات + تغذية مختلطة.
 */
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedIds.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AppTopBar(
            title = stringResource(R.string.app_name),
        )

        when {
            state.loading -> FeedSkeleton()

            state.errorMessage && state.hero == null && state.articles.isEmpty() -> {
                ErrorState(
                    title = stringResource(R.string.error_network_title),
                    message = stringResource(R.string.error_network_message),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = viewModel::reload,
                )
            }

            state.hero == null && state.articles.isEmpty() -> {
                EmptyState(
                    title = stringResource(R.string.empty_category_title),
                    message = stringResource(R.string.empty_category_message),
                    actionLabel = stringResource(R.string.retry),
                    onAction = viewModel::reload,
                )
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = state.loading,
                    onRefresh = viewModel::reload,
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 6.dp),
                    ) {
                        state.hero?.let { hero ->
                            item(key = "hero") {
                                HeroNewsCard(
                                    article = hero,
                                    isSaved = hero.id in savedIds,
                                    onToggleSave = { viewModel.toggleSave(hero) },
                                    onShare = { shareArticle(context, hero) },
                                    onClick = { navController.navigate(Routes.article(hero.id)) },
                                )
                            }
                        }

                        item(key = "chips") {
                            Column {
                                SectionHeader(title = stringResource(R.string.home_for_you))
                                CategoryChipsRow(
                                    categories = viewModel.categories.map { it.id to it.nameAr },
                                    selectedId = state.selectedCategory,
                                    onSelect = { id ->
                                        viewModel.selectCategory(id.takeIf { it != state.selectedCategory })
                                    },
                                )
                            }
                        }

                        items(state.articles.size, key = { state.articles[it].id }) { index ->
                            val article = state.articles[index]
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
                }
            }
        }
    }
}
