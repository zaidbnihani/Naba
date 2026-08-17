package com.nba.plus.domain.usecase

import androidx.paging.PagingData
import com.nba.plus.domain.model.Article
import com.nba.plus.domain.model.FeedTab
import com.nba.plus.domain.repository.CategoriesRepository
import com.nba.plus.domain.repository.FeedSpec
import com.nba.plus.domain.repository.NewsRepository
import com.nba.plus.domain.repository.SourcesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/**
 * يبني تدفق التغذية المُرقّم لتبويب معيّن، ويحقن تفضيلات
 * «لك» (الفئات والمصادر المتابَعة) تلقائيًا عند الحاجة.
 */
class GetFeedUseCase @Inject constructor(
    private val newsRepository: NewsRepository,
    private val sourcesRepository: SourcesRepository,
    private val categoriesRepository: CategoriesRepository,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(tab: FeedTab): Flow<PagingData<Article>> {
        val baseSpec = FeedSpec(tab = tab)
        if (tab != FeedTab.FOR_YOU) {
            return newsRepository.pagedFeed(baseSpec)
        }
        // تبويب «لك» يتبع تغيّرات المتابعات لحظيًا
        return sourcesRepository.observeFollowedIds()
            .flatMapLatest { sourceIds ->
                categoriesRepository.observeFollowedIds()
                    .flatMapLatest { categoryIds ->
                        newsRepository.pagedFeed(
                            baseSpec.copy(
                                forYouSources = sourceIds,
                                forYouCategories = categoryIds,
                            )
                        )
                    }
            }
    }

    /** مواصفات جاهزة لتغذية فئة أو مصدر محدد. */
    suspend fun categorySpec(categoryId: String): FeedSpec =
        FeedSpec(tab = null, categoryId = categoryId)

    suspend fun sourceSpec(sourceId: String): FeedSpec =
        FeedSpec(tab = null, sourceId = sourceId)

    /** الفئات المتابَعة الحالية (استخدامات مساعدة). */
    suspend fun currentForYouSpec(): FeedSpec = FeedSpec(
        tab = FeedTab.FOR_YOU,
        forYouSources = sourcesRepository.observeFollowedIds().first(),
        forYouCategories = categoriesRepository.observeFollowedIds().first(),
    )
}
