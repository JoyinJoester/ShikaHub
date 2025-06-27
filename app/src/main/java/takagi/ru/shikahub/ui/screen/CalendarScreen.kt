package takagi.ru.shikahub.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import takagi.ru.shikahub.data.entity.Shika
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToShikaDetail: (Long) -> Unit,
    viewModel: ShikaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 当前选中日期
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
    // 当前显示的月份
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    
    // 当天的记录数据
    var selectedDateRecords by remember { mutableStateOf<List<Shika>>(emptyList()) }
    
    // 月份选择器是否展开
    var isMonthSelectorExpanded by remember { mutableStateOf(false) }
    
    // 初始化ViewModel数据
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }
    
    // 按日期组织数据
    val recordsByDate = remember(uiState.shikas) {
        uiState.shikas.groupBy { shika ->
            val instant = java.time.Instant.ofEpochMilli(shika.timestamp)
            // 使用ZonedDateTime作为替代，然后转换为LocalDate，这在API 26+上可用
            instant.atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }
    
    // 当选中日期变化时，更新记录列表
    LaunchedEffect(selectedDate, recordsByDate) {
        selectedDateRecords = recordsByDate[selectedDate] ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "日历视图", 
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 月份选择器
            item {
                MonthSelector(
                    currentYearMonth = currentYearMonth,
                    onPreviousMonth = { currentYearMonth = currentYearMonth.minusMonths(1) },
                    onNextMonth = { currentYearMonth = currentYearMonth.plusMonths(1) },
                    isExpanded = isMonthSelectorExpanded,
                    onExpandToggle = { isMonthSelectorExpanded = !isMonthSelectorExpanded }
                )
            }
            
            // 日历网格 - 直接内联，不再需要单独的WeekdayHeader
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    CalendarGrid(
                        yearMonth = currentYearMonth,
                        selectedDate = selectedDate,
                        onDateSelected = { date ->
                            selectedDate = date
                        },
                        recordsByDate = recordsByDate
                    )
                }
            }
            
            // 日期标题卡片
            item {
                val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDate.format(formatter),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${selectedDateRecords.size}条记录",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // 当天记录列表
            if (selectedDateRecords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "暂无记录",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "记录列表",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                items(selectedDateRecords.size) { index ->
                    val record = selectedDateRecords[index]
                    DayRecordItem(
                        record = record,
                        onClick = { onNavigateToShikaDetail(record.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(
    currentYearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    isExpanded: Boolean = false,
    onExpandToggle: () -> Unit = {}
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy年MM月")
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            // 月份选择头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandToggle)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft, 
                        contentDescription = "上个月",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentYearMonth.format(formatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "选择月份",
                        modifier = Modifier.padding(start = 4.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                        contentDescription = "下个月",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // 月份快速选择器
        AnimatedVisibility(visible = isExpanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    val currentYear = YearMonth.now().year
                    val months = (1..12).map { YearMonth.of(currentYear, it) }
                    
                    items(months.size) { index ->
                        val month = months[index]
                        val isSelected = month.year == currentYearMonth.year && 
                                        month.month == currentYearMonth.month
                        
                        MonthChip(
                            month = month.month.getDisplayName(TextStyle.SHORT, Locale.CHINA),
                            isSelected = isSelected,
                            onClick = {
                                onExpandToggle()
                                if (!isSelected) {
                                    // 构建新的YearMonth并导航到该月
                                    val newYearMonth = YearMonth.of(currentYear, month.month)
                                    // 调用导航回调
                                    if (newYearMonth.isBefore(currentYearMonth)) {
                                        // 计算需要向前导航的月数
                                        val monthsBack = currentYearMonth.monthValue - newYearMonth.monthValue
                                        repeat(monthsBack) { onPreviousMonth() }
                                    } else if (newYearMonth.isAfter(currentYearMonth)) {
                                        // 计算需要向后导航的月数
                                        val monthsForward = newYearMonth.monthValue - currentYearMonth.monthValue
                                        repeat(monthsForward) { onNextMonth() }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthChip(
    month: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(36.dp)
            .width(if (isSelected) 64.dp else 56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = month,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    recordsByDate: Map<LocalDate, List<Shika>>
) {
    val today = LocalDate.now()
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()
    
    // 调整一周的开始为周一（Java中1是周一，7是周日）
    val dayOfWeek = firstDayOfMonth.dayOfWeek.value
    
    // 需要显示的总天数（包括前面的空白和当月的天数）
    val totalDays = lastDayOfMonth.dayOfMonth + dayOfWeek - 1
    
    // 计算行数（向上取整）
    val rows = (totalDays + 6) / 7
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            // 星期标题行
            WeekdayHeaderRow()
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 日历网格
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (column in 1..7) {
                        val dayIndex = row * 7 + column - dayOfWeek
                        
                        if (dayIndex < 0 || dayIndex >= lastDayOfMonth.dayOfMonth) {
                            // 非当月的日期显示为空
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            ) {
                                // 空白日期单元格
                            }
                        } else {
                            val date = yearMonth.atDay(dayIndex + 1)
                            val isSelected = date == selectedDate
                            val isToday = date == today
                            val recordCount = recordsByDate[date]?.size ?: 0
                            val isWeekend = date.dayOfWeek.value > 5 // 周六和周日
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // 显示选中或今日状态的背景
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isToday && !isSelected) 1.dp else 0.dp,
                                            color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { onDateSelected(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // 日期数字
                                    Text(
                                        text = (dayIndex + 1).toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                            isToday -> MaterialTheme.colorScheme.primary
                                            isWeekend -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                
                                // 记录徽标指示器
                                if (recordCount > 0) {
                                    val badgeSize = if (recordCount > 9) 18.dp else 16.dp
                                    
                                    Badge(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .offset(x = (-2).dp, y = (-2).dp)
                                            .size(badgeSize),
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(
                                            text = if (recordCount > 99) "99+" else recordCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp
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
private fun WeekdayHeaderRow() {
    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekdays.forEachIndexed { index, day ->
            val isWeekend = index >= 5 // 周六和周日
            
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isWeekend) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayRecordItem(
    record: Shika,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val recordTime = java.time.Instant.ofEpochMilli(record.timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(timeFormatter)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 时间指示器
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = recordTime,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 描述内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                if (record.description != null) {
                    Text(
                        text = record.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 计数显示
                if (record.count > 0) {
                    Text(
                        text = "${record.count}分钟",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
