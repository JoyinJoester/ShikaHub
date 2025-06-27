package takagi.ru.shikahub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.shikahub.data.entity.Shika
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.max
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ShikaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 初始化数据
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }
    
    // 计算总时长（分钟转小时:分钟）
    val totalMinutes = uiState.shikas.sumOf { it.count }
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val totalTimeDisplay = if (hours > 0) {
        "${hours}小时${if (minutes > 0) "${minutes}分钟" else ""}"
    } else {
        "${minutes}分钟"
    }
    
    // 计算本周数据
    val currentWeek = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
    val startOfWeek = currentWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val weekCount = uiState.shikas.count { it.timestamp >= startOfWeek }
    
    // 计算本月数据
    val currentMonth = YearMonth.now()
    val startOfMonth = currentMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val monthCount = uiState.shikas.count { it.timestamp >= startOfMonth }
    
    // 计算总计数据
    val totalCount = uiState.shikas.size
    
    // 构建贡献热图数据（过去12周的数据）
    val contributionData = buildContributionData(uiState.shikas)
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "统计分析", 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 总时长卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "累计时长",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (hours > 0) "$hours" else "$minutes",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = if (hours > 0) {
                                if (minutes > 0) {
                                    ":$minutes"
                                } else {
                                    "小时"
                                }
                            } else {
                                "分钟"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                    }
                    
                    if (hours > 0 && minutes > 0) {
                        Text(
                            text = "小时 : 分钟",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            // 统计概览卡片
            StatsOverviewCard(weekCount, monthCount, totalCount)
            
            // 贡献热图
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "活动热图",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // 绘制贡献图
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        for (week in 0 until 7) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (day in 0 until 17) {
                                    val cellIndex = week + day * 7
                                    if (cellIndex < contributionData.cells.size) {
                                        val cell = contributionData.cells[cellIndex]
                                        ContributionCell(
                                            count = cell.count,
                                            date = cell.date,
                                            colorIntensity = cell.intensity
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    unit: String
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 2.dp, bottom = 1.dp)
            )
        }
    }
}

@Composable
private fun StatsOverviewCard(weekCount: Int, monthCount: Int, totalCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "统计概览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatsItem("本周", weekCount)
                StatsItem("本月", monthCount)
                StatsItem("总计", totalCount)
            }
        }
    }
}

@Composable
private fun StatsItem(label: String, count: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "次",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

@Composable
private fun ContributionCell(
    count: Int,
    date: LocalDate,
    colorIntensity: Int
) {
    val color = getContributionColor(colorIntensity, MaterialTheme.colorScheme.primary)
    
    Tooltip(
        tooltipContent = {
            Column(
                modifier = Modifier
                    .padding(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = if (count > 0) "${count}次记录" else "无记录",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (count > 0) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
    }
}

// 构建贡献热图数据
private fun buildContributionData(shikas: List<Shika>): ContributionMapData {
    // 今天的日期
    val today = LocalDate.now()
    
    // 计算从当前日期向前推119天的日期（共17周）
    val startDate = today.minusDays(118)
    val cells = mutableListOf<ContributionCellData>()
    
    // 按日期分组记录
    val recordsByDate = shikas.groupBy { shika ->
        val instant = java.time.Instant.ofEpochMilli(shika.timestamp)
        instant.atZone(ZoneId.systemDefault()).toLocalDate()
    }
    
    // 找出最大记录数，用于计算热度
    val maxRecords = recordsByDate.values.maxOfOrNull { it.size } ?: 0
    
    // 填充单元格数据（按行优先排列 7x17 共119个单元格）
    // 反转列的顺序，使最近的日期显示在右侧
    for (col in 16 downTo 0) {
        for (row in 0 until 7) {
            // 计算当前日期
            val daysToAdd = (16 - col) * 7 + row
            val currentDate = startDate.plusDays(daysToAdd.toLong())
            
            // 不超过今天的日期
            if (currentDate.isAfter(today)) {
                cells.add(ContributionCellData(0, currentDate, 0))
                continue
            }
            
            // 获取该日期的记录数
            val records = recordsByDate[currentDate] ?: emptyList()
            val count = records.size
            
            // 计算热度等级（0-4，类似GitHub）
            val intensity = when {
                count == 0 -> 0
                maxRecords <= 4 -> count
                else -> {
                    val step = maxRecords / 4.0
                    minOf(4, (count / step).toInt() + 1)
                }
            }
            
            cells.add(ContributionCellData(count, currentDate, intensity))
        }
    }
    
    return ContributionMapData(cells)
}

// 贡献图的颜色计算函数
@Composable
private fun getContributionColor(intensity: Int, baseColor: Color): Color {
    return when (intensity) {
        0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        1 -> baseColor.copy(alpha = 0.3f)
        2 -> baseColor.copy(alpha = 0.5f)
        3 -> baseColor.copy(alpha = 0.7f)
        else -> baseColor.copy(alpha = 0.9f)
    }
}

// 贡献图数据类
private data class ContributionCellData(
    val count: Int,
    val date: LocalDate,
    val intensity: Int
)

private data class ContributionMapData(
    val cells: List<ContributionCellData>
)

// 简易提示框实现
@Composable
private fun Tooltip(
    tooltipContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    
    Box {
        Box(
            modifier = Modifier
                .clickable { showTooltip = !showTooltip }
        ) {
            content()
        }
        
        // 提示内容（实际实现中可能需要改进定位逻辑）
        if (showTooltip) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 4.dp
                ) {
                    tooltipContent()
                }
            }
        }
    }
}