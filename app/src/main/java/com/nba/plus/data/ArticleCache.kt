package com.nba.plus.data

import com.nba.plus.domain.model.Article
import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ذاكرة مؤقتة قصيرة العمر للمقالات المفتوحة في الجلسة الحالية،
 * لتمريرها بين الشاشات دون استعلام الشبكة من جديد.
 */
@Singleton
class ArticleCache @Inject constructor() {

    private val cache = LruCache<String, Article>(64)

    @Synchronized
    fun put(article: Article) {
        cache.put(article.id, article)
    }

    @Synchronized
    fun get(id: String): Article? = cache.get(id)

    @Synchronized
    fun update(article: Article) = put(article)
}
