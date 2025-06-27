package takagi.ru.shikahub.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun GitHubStyleContributionCalendar(
    contributions: List<DailyContribution>,
    modifier: Modifier = Modifier,
    onDayClick: (DailyContribution) -> Unit = {}
) {
    // 将数据重新组织成列（每列7个格子）
    val columns = contributions.chunked(7)
    
    Column(modifier = modifier) {
        // 贡献网格
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            columns.forEach { column ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 1.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    column.forEach { contribution ->
                        ContributionCell(
                            contribution = contribution,
                            onClick = { onDayClick(contribution) }
                        )
                    }
                }
            }
        }

        // 图例
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "贡献度: ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0, 1, 2, 3).forEach { tier ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getContributionColor(tier))
                    )
                }
            }
        }
    }
}

@Composable
private fun ContributionCell(
    contribution: DailyContribution,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        modifier = Modifier
            .size(16.dp)
            .scale(scale)
            .clip(RoundedCornerShape(2.dp))
            .background(getContributionColor(contribution.tier))
            .clickable {
                onClick()
                isHovered = false
            }
            .padding(0.dp)
    )
}

@Composable
private fun getContributionColor(tier: Int): Color {
    return when (tier) {
        0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        1 -> Color(0xFF9BE9A8).copy(alpha = 0.8f)
        2 -> Color(0xFF40C463).copy(alpha = 0.9f)
        else -> Color(0xFF30A14E)
    }
}

data class DailyContribution(
    val date: LocalDate,
    val count: Int,
    val tier: Int
) 