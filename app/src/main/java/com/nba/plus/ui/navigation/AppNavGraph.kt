package com.nba.plus.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.nba.plus.R
import com.nba.plus.ui.screens.article.ArticleDetailScreen
import com.nba.plus.ui.screens.auth.LoginScreen
import com.nba.plus.ui.screens.categories.CategoriesScreen
import com.nba.plus.ui.screens.categories.CategoryFeedScreen
import com.nba.plus.ui.screens.feed.LatestNewsScreen
import com.nba.plus.ui.screens.home.HomeScreen
import com.nba.plus.ui.screens.interactions.InteractionsScreen
import com.nba.plus.ui.screens.onboarding.OnboardingScreen
import com.nba.plus.ui.screens.profile.ProfileScreen
import com.nba.plus.ui.screens.saved.SavedScreen
import com.nba.plus.ui.screens.search.SearchScreen
import com.nba.plus.ui.screens.sources.SourceDetailScreen
import com.nba.plus.ui.screens.sources.SourcesScreen
import com.nba.plus.ui.theme.Purple

/** مسارات التنقل. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val LATEST = "latest"
    const val SOURCES = "sources"
    const val CATEGORIES = "categories"
    const val PROFILE = "profile"
    const val SEARCH = "search"
    const val SAVED = "saved"
    const val INTERACTIONS = "interactions"
    const val LOGIN = "login"

    private const val ARTICLE_BASE = "article"
    private const val SOURCE_BASE = "source"
    private const val CATEGORY_BASE = "category"

    fun article(id: String) = "$ARTICLE_BASE/$id"
    fun source(id: String) = "$SOURCE_BASE/$id"
    fun category(id: String) = "$CATEGORY_BASE/$id"

    val bottomRoutes: Set<String> = setOf(HOME, LATEST, SOURCES, CATEGORIES, PROFILE)
}

private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/** جذر التطبيق: Scaffold + شريط سفلي + رسم التنقل. */
@Composable
fun AppRoot(startDestination: String) {
    val navController = androidx.navigation.compose.rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.bottomRoutes

    val tabs = listOf(
        BottomTab(Routes.HOME, R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
        BottomTab(Routes.LATEST, R.string.tab_latest, Icons.Filled.Newspaper, Icons.Outlined.Newspaper),
        BottomTab(Routes.SOURCES, R.string.tab_sources, Icons.Filled.Source, Icons.Outlined.Source),
        BottomTab(Routes.CATEGORIES, R.string.tab_categories, Icons.Filled.GridView, Icons.Outlined.GridView),
        BottomTab(Routes.PROFILE, R.string.tab_profile, Icons.Filled.Person, Icons.Outlined.Person),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.labelRes),
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Purple,
                                selectedTextColor = Purple,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unqualifiedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(navController)
            }
            composable(Routes.HOME) {
                HomeScreen(navController)
            }
            composable(Routes.LATEST) {
                LatestNewsScreen(navController)
            }
            composable(Routes.SOURCES) {
                SourcesScreen(navController)
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(navController)
            }
            composable(Routes.PROFILE) {
                ProfileScreen(navController)
            }
            composable(Routes.SEARCH) {
                SearchScreen(navController)
            }
            composable(Routes.SAVED) {
                SavedScreen(navController)
            }
            composable(Routes.INTERACTIONS) {
                InteractionsScreen(navController)
            }
            composable(Routes.LOGIN) {
                LoginScreen(navController)
            }
            composable(
                route = "article/{articleId}",
                arguments = listOf(navArgument("articleId") { type = NavType.StringType }),
            ) {
                ArticleDetailScreen(navController)
            }
            composable(
                route = "source/{sourceId}",
                arguments = listOf(navArgument("sourceId") { type = NavType.StringType }),
            ) {
                SourceDetailScreen(navController)
            }
            composable(
                route = "category/{categoryId}",
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
            ) {
                CategoryFeedScreen(navController)
            }
        }
    }
}
