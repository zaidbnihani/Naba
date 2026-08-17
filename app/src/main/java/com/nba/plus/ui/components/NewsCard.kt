package com.nba.plus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nba.plus.R
import com.nba.plus.domain.model.Article
import com.nba.plus.ui.theme.BreakingRed
import com.nba.plus.ui.theme.LocalHeadlineScale
import com.nba.plus.ui.util.TimeFormat

/**
 * بطاقة الخبر القياسية في التغذيات: مصدر + وقت، عنوان (سطران)،
 * صورة كبيرة، ثم صف التفاعلات — كما في لقطات التصميم.
 */
@Composable
fun NewsCard(
    article: Article,
    isLiked: Boolean,
    isSaved: Boolean,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val headlineScale = LocalHeadlineScale.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            ArticleMetaRow(
                sourceName = article.sourceName,
                sourceIconUrl = article.sourceIconUrl,
                publishedAt = article.publishedAt,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = (17 * headlineScale).sp,
                    lineHeight = ((17 * headlineScale) * 1.55).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (article.isBreaking) {
                    BreakingBadge()
                }
            }

            if (article.imageUrl != null) {
                ShimmerAsyncImage(
                    model = article.imageUrl,
                    contentDescription = article.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .padding(top = 10.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.medium,
                        ),
                )
            }

            ArticleActionRow(
                commentCount = article.commentCount,
                likeCount = article.likeCount + if (isLiked) 1 else 0,
                isLiked = isLiked,
                isSaved = isSaved,
                onToggleLike = onToggleLike,
                onToggleSave = onToggleSave,
                onShare = onShare,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** شارة «عاجل» الحمراء. */
@Composable
fun BreakingBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(BreakingRed, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = stringResource(R.string.badge_breaking),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * بطاقة الخبر الرئيسية (Hero) في أعلى الشاشة الرئيسية:
 * صورة كاملة متدرّجة مع العنوان والشارة فوقها.
 */
@Composable
fun HeroNewsCard(
    article: Article,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val headlineScale = LocalHeadlineScale.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .aspectRatio(4f / 4.4f)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        ShimmerAsyncImage(
            model = article.imageUrl,
            contentDescription = article.title,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color(0xB3131420),
                            Color(0xE6131420),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (article.isBreaking) BreakingBadge()
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = (22 * headlineScale).sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = article.sourceName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFA29BFE),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.fillMaxWidth(0.4f))
                Text(
                    text = TimeFormat.relative(LocalContext.current, article.publishedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFC9C9D9),
                )
            }
        }
    }
}

/** بطاقة مصغّرة أفقية للقوائم الثانوية والمقالات ذات الصلة. */
@Composable
fun CompactNewsCard(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = article.sourceName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        ShimmerAsyncImage(
            model = article.imageUrl,
            contentDescription = article.title,
            modifier = Modifier
                .height(76.dp)
                .aspectRatio(1.35f)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
        )
    }
}
