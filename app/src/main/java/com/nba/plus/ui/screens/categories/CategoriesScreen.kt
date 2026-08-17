package com.nba.plus.ui.screens.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.nba.plus.domain.repository.CategoriesRepository
import com.nba.plus.ui.components.AppTopBar
import com.nba.plus.ui.components.CategoryCard
import com.nba.plus.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoriesRepository: CategoriesRepository,
) : ViewModel() {

    val categories = categoriesRepository.getCategories()

    val followedIds: StateFlow<Set<String>> = categoriesRepository.observeFollowedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleFollow(categoryId: String) {
        viewModelScope.launch {
            val isFollowed = categoryId in followedIds.value
            categoriesRepository.setFollowed(categoryId, !isFollowed)
        }
    }
}

/**
 * شاشة «مجالات»: شبكة عمودين من بطاقات الفئات بصور كاملة
 * واسم الفئة فوقها — كما في لقطة التصميم.
 */
@Composable
fun CategoriesScreen(
    navController: NavController,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        AppTopBar(title = stringResource(R.string.tab_categories))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(viewModel.categories, key = { it.id }) { category ->
                CategoryCard(
                    category = category,
                    onClick = { navController.navigate(Routes.category(category.id)) },
                )
            }
        }
    }
}
