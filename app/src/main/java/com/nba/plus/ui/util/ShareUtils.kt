package com.nba.plus.ui.util

import android.content.Context
import android.content.Intent
import com.nba.plus.R
import com.nba.plus.domain.model.Article

/** مشاركة الخبر عبر لوحة المشاركة الأصلية في أندرويد. */
fun shareArticle(context: Context, article: Article) {
    val text = buildString {
        append(article.title)
        if (article.url.isNotBlank()) {
            append("\n")
            append(article.url)
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.share_article))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
