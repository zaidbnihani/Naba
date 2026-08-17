package com.nba.plus.data.repository

import com.nba.plus.domain.model.NewsCategory

/** الفئات المعروضة في شاشة «مجالات». */
object DefaultCategories {

    val all: List<NewsCategory> = listOf(
        NewsCategory("ai", "نبض الذكاء الاصطناعي", "AI", "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=640&q=80"),
        NewsCategory("automotive", "نبض السيارات", "Automotive", "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=640&q=80"),
        NewsCategory("technology", "نبض التكنولوجيا", "Technology", "https://images.unsplash.com/photo-1518770660439-4636190af475?w=640&q=80"),
        NewsCategory("health", "نبض الصحة", "Health", "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=640&q=80"),
        NewsCategory("business", "نبض الاقتصاد", "Economy", "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=640&q=80"),
        NewsCategory("travel", "نبض السياحة", "Travel", "https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff?w=640&q=80"),
        NewsCategory("food", "نبض الطهي", "Cooking", "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=640&q=80"),
        NewsCategory("science", "نبض العلوم", "Science", "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=640&q=80"),
        NewsCategory("sports", "نبض الرياضة", "Sports", "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=640&q=80"),
        NewsCategory("politics", "نبض السياسة", "Politics", "https://images.unsplash.com/photo-1541872703-74c5e44368f9?w=640&q=80"),
    )
}
