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
import takagi.ru.shikahub.ui.components.DailyContribution
import takagi.ru.shikahub.ui.components.GitHubStyleContributionCalendar
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.max

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
    
    var selectedContribution by remember { mutableStateOf<DailyContribution?>(null) }
    
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
            
            // GitHub风格贡献热图卡片
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
                    
                    GitHubStyleContributionCalendar(
                        contributions = contributionData,
                        modifier = Modifier.fillMaxWidth(),
                        onDayClick = { contribution ->
                            selectedContribution = contribution
                        }
                    )
                }
            }
            
            // 选中日期的详细信息
            selectedContribution?.let { contribution ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = contribution.date.format(
                                DateTimeFormatter.ofPattern("yyyy年MM月dd日")
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (contribution.count > 0) 
                                "记录了 ${contribution.count} 次" 
                            else 
                                "暂无记录",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
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

// 构建贡献热图数据
private fun buildContributionData(shikas: List<Shika>): List<DailyContribution> {
    // 今天的日期
    val today = LocalDate.now()
    
    // 计算开始日期：从今天向前推算，确保今天在正确位置
    // 首先计算今天是星期几（1-7，周一到周日）
    val currentDayOfWeek = today.dayOfWeek.value
    
    // 计算需要多少个完整的周才能让今天在倒数第二行
    val weeksNeeded = 7  // 只显示最近7周
    val daysToSubtract = (weeksNeeded - 1) * 7 + (currentDayOfWeek - 1)  // 减去天数使得周一在第一列
    
    // 计算开始日期
    val startDate = today.minusDays(daysToSubtract.toLong())
    
    android.util.Log.d("StatsScreen", "今天: $today")
    android.util.Log.d("StatsScreen", "当前星期几: $currentDayOfWeek")
    android.util.Log.d("StatsScreen", "需要减去的天数: $daysToSubtract")
    android.util.Log.d("StatsScreen", "开始日期: $startDate")
    
    // 按日期分组记录，使用timestamp字段
    val recordsByDate = shikas
        .filter { it.timestamp > 0 } // 过滤掉无效时间戳
        .groupBy { shika ->
            // 转换时间戳为当地时区的日期
            val instant = java.time.Instant.ofEpochMilli(shika.timestamp)
            val zoneId = ZoneId.systemDefault()
            instant.atZone(zoneId).toLocalDate()
        }
    
    // 调试日志
    android.util.Log.d("StatsScreen", "总记录数: ${shikas.size}")
    android.util.Log.d("StatsScreen", "有效日期记录数: ${recordsByDate.size}")
    recordsByDate.forEach { (date, records) ->
        android.util.Log.d("StatsScreen", "日期: $date, 记录数: ${records.size}, 时间戳: ${records.map { it.timestamp }}")
    }
    
    // 生成所有日期的贡献数据
    val contributions = mutableListOf<DailyContribution>()
    
    // 从最早的日期开始，按照从左到右，从上到下的顺序生成数据
    for (weekIndex in 0 until weeksNeeded) {
        for (dayOfWeek in 1..7) { // 从周一(1)到周日(7)
            val daysFromStart = weekIndex * 7 + (dayOfWeek - 1)
            val date = startDate.plusDays(daysFromStart.toLong())
            
            val records = recordsByDate[date] ?: emptyList()
            val count = records.size
            
            // 调试日志
            if (count > 0) {
                android.util.Log.d("StatsScreen", "发现记录 - 日期: $date, 数量: $count, 时间戳: ${records.map { it.timestamp }}")
            }
            
            // 计算热度等级（0-3）
            val tier = when {
                count == 0 -> 0
                count <= 3 -> 1
                count <= 6 -> 2
                else -> 3
            }
            
            contributions.add(DailyContribution(date, count, tier))
        }
    }
    
    return contributions
}