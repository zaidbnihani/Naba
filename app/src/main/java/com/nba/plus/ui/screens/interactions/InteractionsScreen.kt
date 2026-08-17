package com.nba.plus.ui.screens.interactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
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
import com.nba.plus.domain.model.Article
import com.nba.plus.domain.repository.InteractionsRepository
import com.nba.plus.ui.components.AppTopBar
import com.nba.plus.ui.components.CompactNewsCard
import com.nba.plus.ui.components.EmptyState
import com.nba.plus.ui.components.SectionHeader
import com.nba.plus.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InteractionsViewModel @Inject constructor(
    private val interactionsRepository: InteractionsRepository,
) : ViewModel() {

    val likedArticles: StateFlow<List<Article>> = interactionsRepository.observeLiked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleLike(article: Article) {
        viewModelScope.launch { interactionsRepository.toggleLike(article) }
    }
}

/** شاشة «التفاعلات»: الأخبار التي أعجب المستخدم. */
@Composable
fun InteractionsScreen(
    navController: NavController,
    viewModel: InteractionsViewModel = hiltViewModel(),
) {
    val liked by viewModel.likedArticles.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AppTopBar(
            title = stringResource(R.string.interactions_title),
            showBack = true,
            onBack = { navController.popBackStack() },
        )

        if (liked.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.ThumbUpOffAlt,
                title = stringResource(R.string.empty_interactions_title),
                message = stringResource(R.string.empty_interactions_message),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                item(key = "header") {
                    SectionHeader(title = stringResource(R.string.interactions_likes))
                }
                items(liked.size, key = { liked[it].id }) { index ->
                    val article = liked[index]
                    CompactNewsCard(
                        article = article,
                        onClick = { navController.navigate(Routes.article(article.id)) },
                    )
                }
            }
        }
    }
}
