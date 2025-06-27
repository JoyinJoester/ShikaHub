package takagi.ru.shikahub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.shikahub.data.entity.Shika
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddShika: () -> Unit,
    onNavigateToShikaDetail: (Long) -> Unit,
    viewModel: ShikaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    // 初始化ViewModel数据（如果为空）
    LaunchedEffect(Unit) {
        // 从数据库加载最新数据
        viewModel.refreshData()
        
        // 移除自动创建卡片的逻辑
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // 直接添加一个新卡片，不需要选择时间
                    val now = System.currentTimeMillis()
                    val currentTime = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    
                    val newShika = Shika(
                        id = now,
                        title = "时间记录",
                        description = "创建于 $currentTime",
                        count = 0, // 未计时，初始显示0分钟
                        createdAt = now,
                        updatedAt = now,
                        timestamp = now
                    )
                    
                    // 添加到ViewModel并持久化到数据库
                    viewModel.addShika(newShika)
                    
                    // 可选：直接进入详情页面
                    onNavigateToShikaDetail(newShika.id)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加时间卡片")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 自定义顶栏
                CustomTopBar(onClear = {
                    // 清空记录功能 - 使用ViewModel的清空功能
                    viewModel.clearAllShikas()
                })
                
                // 内容区域
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 统计卡片
                    item {
                        StatsCard(
                            todayCount = uiState.shikas.size,
                            totalTime = formatTotalTime(uiState.shikas)
                        )
                    }
                    
                    if (uiState.shikas.isEmpty()) {
                        item {
                            EmptyState(onAddClick = {
                                // 直接添加一个本地记录，无需选择时间
                                val currentTime = java.time.LocalDateTime.now()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                
                                val newShika = Shika(
                                    id = System.currentTimeMillis(),
                                    title = "时间记录",
                                    description = "创建于 $currentTime",
                                    count = 0, // 未计时
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis(),
                                    timestamp = System.currentTimeMillis()
                                )
                                
                                // 添加到ViewModel并持久化到数据库
                                viewModel.addShika(newShika)
                                
                                // 可选：直接进入详情页面
                                onNavigateToShikaDetail(newShika.id)
                            })
                        }
                    } else {
                        item {
                            Text(
                                text = "时间记录 (${uiState.shikas.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(uiState.shikas) { shika ->
                            TimeRecordItem(
                                shika = shika,
                                onItemClick = { 
                                    onNavigateToShikaDetail(shika.id) 
                                },
                                onDelete = {
                                    // 使用ViewModel的删除功能
                                    viewModel.deleteShika(shika.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomTopBar(onClear: () -> Unit) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ShikaHub",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            // 只保留清空按钮
            IconButton(
                onClick = { showConfirmDialog = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "清空记录",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    
    // 确认对话框
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("确认清空") },
            text = { Text("确定要清空所有时间记录吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        onClear()
                        showConfirmDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun StatsCard(
    todayCount: Int,
    totalTime: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "今日: ${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("记录次数", todayCount.toString())
                StatItem("累计时间", totalTime)
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "还没有记录",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "点击下方按钮添加时间记录",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("开导")
            }
        }
    }
}

@Composable
private fun QuickTimeButton(text: String, minutes: Int, onClick: (Int) -> Unit) {
    OutlinedButton(
        onClick = { onClick(minutes) },
        modifier = Modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRecordItem(
    shika: Shika,
    onItemClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onItemClick,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：时间和描述
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDateTime(shika.timestamp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 添加时长标签 - 从描述中提取时间格式
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        // 从描述中提取时间格式或使用count字段
                        val timeDisplay = when {
                            // 如果描述中包含"计时完成："或"手动设置："，则提取后面的时间格式
                            shika.description?.contains("计时完成：") == true -> {
                                shika.description.substringAfter("计时完成：")
                            }
                            shika.description?.contains("手动设置：") == true -> {
                                shika.description.substringAfter("手动设置：")
                            }
                            // 否则使用count字段显示分钟数
                            shika.count > 0 -> "${shika.count}分钟"
                            else -> "未计时"
                        }
                        
                        Text(
                            text = timeDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                if (shika.description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    // 显示描述，但如果包含计时信息，则只显示前半部分
                    val displayDescription = when {
                        shika.description.contains("计时完成：") -> {
                            "计时完成"
                        }
                        shika.description.contains("手动设置：") -> {
                            "手动设置"
                        }
                        else -> shika.description
                    }
                    Text(
                        text = displayDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 右侧：删除按钮
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除记录",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatDateTime(timestamp: Long): String {
    if (timestamp == 0L) return "未知时间"
    
    val dateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    
    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
}

private fun formatTotalTime(shikas: List<Shika>): String {
    // 计算总秒数
    var totalSeconds = 0L
    
    shikas.forEach { shika ->
        // 尝试从描述中提取时间格式
        val timeStr = when {
            shika.description?.contains("计时完成：") == true -> {
                shika.description.substringAfter("计时完成：")
            }
            shika.description?.contains("手动设置：") == true -> {
                shika.description.substringAfter("手动设置：")
            }
            else -> null
        }
        
        if (timeStr != null && timeStr.matches(Regex("\\d{2}:\\d{2}:\\d{2}"))) {
            // 如果是HH:MM:SS格式，解析时间
            val parts = timeStr.split(":")
            if (parts.size == 3) {
                try {
                    val hours = parts[0].toInt()
                    val minutes = parts[1].toInt()
                    val seconds = parts[2].toInt()
                    totalSeconds += hours * 3600L + minutes * 60L + seconds
                } catch (e: Exception) {
                    // 解析失败，使用count字段作为分钟数
                    totalSeconds += shika.count * 60L
                }
            } else {
                // 格式不正确，使用count字段作为分钟数
                totalSeconds += shika.count * 60L
            }
        } else {
            // 没有时间格式，使用count字段作为分钟数
            totalSeconds += shika.count * 60L
        }
    }
    
    // 格式化总时间
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return when {
        hours > 0 -> {
            if (seconds > 0) {
                "${hours}小时${minutes}分${seconds}秒"
            } else if (minutes > 0) {
                "${hours}小时${minutes}分钟"
            } else {
                "${hours}小时"
            }
        }
        minutes > 0 -> {
            if (seconds > 0) {
                "${minutes}分${seconds}秒"
            } else {
                "${minutes}分钟"
            }
        }
        else -> "${seconds}秒"
    }
}
