package com.nba.plus.ui.screens.article

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nba.plus.R
import com.nba.plus.domain.model.Article
import com.nba.plus.domain.model.HeadlineFontSize
import com.nba.plus.domain.model.UserPreferences
import com.nba.plus.domain.repository.NewsRepository
import com.nba.plus.domain.repository.SavedArticlesRepository
import com.nba.plus.domain.repository.UserPreferencesRepository
import com.nba.plus.ui.components.AppTopBar
import com.nba.plus.ui.components.BreakingBadge
import com.nba.plus.ui.components.CompactNewsCard
import com.nba.plus.ui.components.ErrorState
import com.nba.plus.ui.components.SectionHeader
import com.nba.plus.ui.components.ShimmerAsyncImage
import com.nba.plus.ui.navigation.Routes
import com.nba.plus.ui.util.shareArticle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticleDetailUiState(
    val loading: Boolean = true,
    val errorMessage: Boolean = false,
    val article: Article? = null,
    val related: List<Article> = emptyList(),
    val isSaved: Boolean = false,
    val isLiked: Boolean = false,
)

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newsRepository: NewsRepository,
    private val savedArticlesRepository: SavedArticlesRepository,
    private val interactionsRepository: com.nba.plus.domain.repository.InteractionsRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val articleId: String = savedStateHandle.get<String>("articleId").orEmpty()

    private val _state = MutableStateFlow(ArticleDetailUiState())
    val state: StateFlow<ArticleDetailUiState> = _state.asStateFlow()

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, errorMessage = false) }
        viewModelScope.launch {
            runCatching { newsRepository.getArticle(articleId) }
                .onSuccess { article ->
                    if (article == null) {
                        _state.update { it.copy(loading = false, errorMessage = true) }
                        return@launch
                    }
                    val saved = savedArticlesRepository.isSaved(article.id)
                    val related = runCatching {
                        newsRepository.getRelated(article)
                    }.getOrDefault(emptyList())
                    _state.update {
                        it.copy(
                            loading = false,
                            article = article,
                            related = related,
                            isSaved = saved,
                        )
                    }
                }
                .onFailure {
                    _state.update { s -> s.copy(loading = false, errorMessage = true) }
                }
        }
        viewModelScope.launch {
            interactionsRepository.observeLikedArticleIds().collect { likedIds ->
                _state.update { it.copy(isLiked = articleId in likedIds) }
            }
        }
    }

    fun toggleSave() {
        val article = _state.value.article ?: return
        viewModelScope.launch {
            val saved = savedArticlesRepository.toggleSaved(article)
            _state.update { it.copy(isSaved = saved) }
        }
    }

    fun toggleLike() {
        val article = _state.value.article ?: return
        viewModelScope.launch {
            val liked = interactionsRepository.toggleLike(article)
            _state.update { it.copy(isLiked = liked) }
        }
    }

    /** تدوير حجم الخط بين المستويات الأربعة مع التخزين الدائم. */
    fun cycleFontSize() {
        viewModelScope.launch {
            preferencesRepository.update { it.cycleFont() }
        }
    }
}

/**
 * شاشة تفاصيل الخبر: صورة رئيسية، بيانات المصدر، نص مقروء،
 * أزرار عائمة (حفظ/مشاركة/حجم الخط/متصفح) وأخبار ذات صلة أفقيًا.
 */
@Composable
fun ArticleDetailScreen(
    navController: NavController,
    viewModel: ArticleDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val fontScale = prefs.headlineFontSize.scale

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AppTopBar(
            title = "",
            showBack = true,
            onBack = { navController.popBackStack() },
            actions = {
                state.article?.let { article ->
                    IconButton(onClick = { shareArticle(context, article) }) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = null)
                    }
                }
            },
        )

        when {
            state.loading -> ArticleDetailSkeleton()

            state.errorMessage || state.article == null -> {
                ErrorState(
                    title = stringResource(R.string.error_generic_title),
                    message = stringResource(R.string.error_generic_message),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = viewModel::load,
                )
            }

            else -> {
                val article = state.article!!
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        item(key = "hero") {
                            Box {
                                ShimmerAsyncImage(
                                    model = article.imageUrl,
                                    contentDescription = article.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(230.dp),
                                )
                                if (article.isBreaking) {
                                    BreakingBadge(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .align(Alignment.TopStart),
                                    )
                                }
                            }
                        }

                        item(key = "header") {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(
                                    text = article.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontSize = (22 * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = article.sourceName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Text(
                                    text = context.resources.getQuantityString(
                                        R.plurals.read_time_minutes,
                                        readMinutes(article),
                                        readMinutes(article),
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }

                        item(key = "body") {
                            Column(
                                Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (article.description.isNotBlank()) {
                                    Text(
                                        text = article.description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = (16 * fontScale).sp,
                                    )
                                }
                                article.content.split("\n\n")
                                    .filter { it.isNotBlank() }
                                    .forEach { paragraph ->
                                        Text(
                                            text = paragraph.trim(),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontSize = (16 * fontScale).sp,
                                            lineHeight = (28 * fontScale).sp,
                                        )
                                    }
                            }
                        }

                        if (article.url.isNotBlank()) {
                            item(key = "browser_link") {
                                Text(
                                    text = stringResource(R.string.open_in_browser),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                )
                            }
                        }

                        if (state.related.isNotEmpty()) {
                            item(key = "related_header") {
                                SectionHeader(title = stringResource(R.string.article_related))
                            }
                            item(key = "related_row") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    items(state.related.size, key = { state.related[it].id }) { index ->
                                        val related = state.related[index]
                                        Box(Modifier.width(250.dp)) {
                                            CompactNewsCard(
                                                article = related,
                                                onClick = {
                                                    navController.navigate(Routes.article(related.id))
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 20.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 6.dp,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            IconButton(
                                onClick = {
                                    if (article.url.isNotBlank()) openInBrowser(context, article.url)
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInBrowser,
                                    contentDescription = stringResource(R.string.open_in_browser),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { shareArticle(context, article) }) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = stringResource(R.string.share_article),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = viewModel::cycleFontSize) {
                                Icon(
                                    imageVector = Icons.Filled.FormatSize,
                                    contentDescription = stringResource(R.string.settings_headline_font_size),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = viewModel::toggleLike) {
                                Icon(
                                    imageVector = if (state.isLiked) androidx.compose.material.icons.filled.Favorite else androidx.compose.material.icons.filled.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (state.isLiked) com.nba.plus.ui.theme.BreakingRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = viewModel::toggleSave) {
                                Icon(
                                    imageVector = if (state.isSaved) Icons.Filled.BookmarkAdded else Icons.Filled.Bookmark,
                                    contentDescription = stringResource(R.string.profile_favorites),
                                    tint = if (state.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun UserPreferences.cycleFont(): UserPreferences {
    val order = HeadlineFontSize.entries
    val next = order[(order.indexOf(headlineFontSize) + 1) % order.size]
    return copy(headlineFontSize = next)
}

private fun readMinutes(article: Article): Int {
    val words = (article.content.ifBlank { article.description }).split(Regex("\\s+")).size
    return (words / 180).coerceIn(1, 30)
}

private fun openInBrowser(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

/** هيكل تحميل لشاشة التفاصيل. */
@Composable
private fun ArticleDetailSkeleton() {
    Column(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small),
        )
        Box(
            Modifier
                .fillMaxWidth(0.6f)
                .height(14.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small),
        )
    }
}
