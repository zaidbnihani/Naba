package com.nba.plus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nba.plus.R
import com.nba.plus.domain.model.NewsCategory
import com.nba.plus.domain.model.Source
import com.nba.plus.ui.theme.SuccessGreen
import com.nba.plus.ui.util.TimeFormat

/**
 * بطاقة فئة بصورة كاملة واسم الفئة فوقها (شبكة «مجالات»).
 */
@Composable
fun CategoryCard(
    category: NewsCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1.15f)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        ShimmerAsyncImage(
            model = category.imageUrl,
            contentDescription = category.nameAr,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color(0xCC131420),
                    ),
                ),
        )
        Text(
            text = category.nameAr,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        )
    }
}

/**
 * صف مصدر في شاشة «مصادري»: الاسم + شعار في الطرف.
 */
@Composable
fun SourceListItem(
    source: Source,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = source.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    R.string.followers_count,
                    TimeFormat.compact(source.followersCount),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            SourceAvatar(name = source.name, iconUrl = source.iconUrl, size = 40.dp)
        }
    }
}

/**
 * زر متابعة/إلغاء متابعة (متدرّج بنفسجي عند المتابعة).
 */
@Composable
fun FollowButton(
    isFollowing: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isFollowing) {
        OutlinedButton(
            onClick = onToggle,
            modifier = modifier,
            shape = RoundedCornerShape(50),
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(stringResource(R.string.following))
        }
    } else {
        Button(
            onClick = onToggle,
            modifier = modifier,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.PersonAddAlt1,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(stringResource(R.string.follow))
        }
    }
}

/**
 * مفتاح تبديل أخضر عند التفعيل — مطابق لمفاتيح لقطات التصميم.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = SuccessGreen,
            checkedBorderColor = SuccessGreen,
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
}

/** أيقونة جرس تُستخدم في مفاتيح تنبيهات المصدر. */
@Composable
fun AlertsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.NotificationsActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
