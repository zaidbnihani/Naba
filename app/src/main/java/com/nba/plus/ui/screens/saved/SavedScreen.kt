package com.nba.plus.ui.screens.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
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
import com.nba.plus.domain.repository.InteractionsRepository
import com.nba.plus.domain.repository.NewsRepository
import com.nba.plus.domain.repository.SavedArticlesRepository
import com.nba.plus.ui.components.EmptyState
import com.nba.plus.ui.components.NewsCard
import com.nba.plus.ui.navigation.Routes
import com.nba.plus.ui.util.shareArticle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val savedArticlesRepository: SavedArticlesRepository,
) : ViewModel() {

    val savedArticles: StateFlow<List<Article>> = savedArticlesRepository.observeSaved()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleSave(article: Article) {
        viewModelScope.launch { savedArticlesRepository.toggleSaved(article) }
    }
}

/** شاشة «المفضلة»: المقالات المحفوظة مع حالة فراغ. */
@Composable
fun SavedScreen(
    navController: NavController,
    viewModel: SavedViewModel = hiltViewModel(),
) {
    val saved by viewModel.savedArticles.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        com.nba.plus.ui.components.AppTopBar(
            title = stringResource(R.string.profile_favorites),
            showBack = true,
            onBack = { navController.popBackStack() },
        )

        if (saved.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.BookmarkBorder,
                title = stringResource(R.string.empty_saved_title),
                message = stringResource(R.string.empty_saved_message),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(saved.size, key = { saved[it].id }) { index ->
                    val article = saved[index]
                    NewsCard(
                        article = article,
                        isLiked = false,
                        isSaved = true,
                        onToggleLike = { },
                        onToggleSave = { viewModel.toggleSave(article) },
                        onShare = { shareArticle(context, article) },
                        onClick = { navController.navigate(Routes.article(article.id)) },
                    )
                }
            }
        }
    }
}
