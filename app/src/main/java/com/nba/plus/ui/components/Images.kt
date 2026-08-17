package com.nba.plus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.nba.plus.ui.theme.shimmer
import com.nba.plus.ui.util.TimeFormat
import java.time.Instant

/** صورة مع حالة تحميل وميض وحالة خطأ رشيقة. */
@Composable
fun ShimmerAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            Box(Modifier.fillMaxSize().shimmer())
        },
        error = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
        },
    )
}

/** شعار مصدر دائري مع بديل حرفي عند غياب الصورة. */
@Composable
fun SourceAvatar(
    name: String,
    iconUrl: String?,
    size: Dp = 36.dp,
) {
    if (iconUrl.isNullOrBlank()) {
        val letter = name.trim().take(1).ifEmpty { "•" }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
    } else {
        ShimmerAsyncImage(
            model = iconUrl,
            contentDescription = name,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
        )
    }
}

/** صف البيانات الوصفية: شعار المصدر + الاسم + الوقت النسبي. */
@Composable
fun ArticleMetaRow(
    sourceName: String,
    sourceIconUrl: String?,
    publishedAt: Instant,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 22.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SourceAvatar(name = sourceName, iconUrl = sourceIconUrl, size = avatarSize)
        Text(
            text = sourceName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "•",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = TimeFormat.relative(LocalContext.current, publishedAt),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
