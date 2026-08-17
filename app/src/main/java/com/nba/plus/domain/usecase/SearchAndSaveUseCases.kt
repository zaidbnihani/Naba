package com.nba.plus.domain.usecase

import com.nba.plus.domain.model.Article
import com.nba.plus.domain.repository.NewsRepository
import com.nba.plus.domain.repository.SavedArticlesRepository
import javax.inject.Inject

/** بحث مع تسجيل الاستعلام في السجل الحديث. */
class SearchNewsUseCase @Inject constructor(
    private val newsRepository: NewsRepository,
) {
    suspend operator fun invoke(query: String): List<Article> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        newsRepository.recordSearch(trimmed)
        return newsRepository.search(trimmed)
    }
}

/** تبديل حفظ مقال (محليًا + مزامنة سحابية عند توفرها). */
class ToggleSaveUseCase @Inject constructor(
    private val savedArticlesRepository: SavedArticlesRepository,
) {
    suspend operator fun invoke(article: Article): Boolean =
        savedArticlesRepository.toggleSaved(article)
}
