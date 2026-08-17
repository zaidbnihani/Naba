package com.nba.plus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nba.plus.ui.theme.Purple
import com.nba.plus.ui.theme.PurpleContainer
import com.nba.plus.ui.theme.OnPurpleContainer

/**
 * صف تبويبات علوية على شكل حبوب (pills) — كما في لقطة التغذية:
 * الأكثر قراءة | آخر الأخبار | عاجل | لك
 */
@Composable
fun TopTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .background(
                        if (selected) Purple else PurpleContainer,
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) Color.White else OnPurpleContainer,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/** ترويسة قسم (عنوان صغير فوق قائمة). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * صف رقائق الفئات في الشاشة الرئيسية.
 */
@Composable
fun CategoryChipsRow(
    categories: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { (id, name) ->
            val selected = id == selectedId
            Box(
                modifier = Modifier
                    .background(
                        if (selected) Purple else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/** رقائق المواضيع الرائجة في البحث. */
@Composable
fun TrendingChips(
    topics: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        topics.chunked(3).forEach { rowTopics ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTopics.forEach { topic ->
                    Box(
                        modifier = Modifier
                            .background(PurpleContainer, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.labelLarge,
                            color = OnPurpleContainer,
                        )
                    }
                }
            }
        }
    }
}
