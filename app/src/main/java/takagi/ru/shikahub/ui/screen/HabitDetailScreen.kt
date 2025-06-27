package takagi.ru.shikahub.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import takagi.ru.shikahub.data.entity.Shika
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShikaDetailScreen(
    shikaId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ShikaViewModel = hiltViewModel()
) {
    // 获取主页面中已有的记录
    val uiState by viewModel.uiState.collectAsState()
    
    // 从主页面的数据中查找当前ID的记录，如果没有则创建新的
    var shika by remember { 
        val existingShika = uiState.shikas.find { it.id == shikaId }
        
        if (existingShika != null) {
            // 使用已有的记录
            android.util.Log.d("ShikaDetailScreen", "使用已有记录: ID=$shikaId, count=${existingShika.count}")
            mutableStateOf(existingShika)
        } else {
            // 创建新记录
            val now = System.currentTimeMillis()
            android.util.Log.d("ShikaDetailScreen", "创建新记录: ID=$shikaId")
            mutableStateOf(
                Shika(
                    id = shikaId,
                    title = "时间记录",
                    description = "点击按钮开始计时或手动设置时间",
                    count = 0, // 初始未计时
                    createdAt = now,
                    updatedAt = now,
                    timestamp = now
                )
            )
        }
    }
    
    // 计时状态
    val coroutineScope = rememberCoroutineScope()
    var isTimerRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var timerStartTime by remember { mutableStateOf(0L) }
    var savedElapsedTime by remember { mutableStateOf(0L) } // 用于暂停时保存时间
    
    // 记录计时结果的状态，改为记录秒数
    var timerResult by remember { mutableStateOf(0) }
    
    // 添加一个状态来跟踪是否有计时完成
    var hasTimerCompleted by remember { mutableStateOf(false) }
    
    // 时间格式化函数
    val formatTime = { timeInSeconds: Long ->
        val hours = timeInSeconds / 3600
        val minutes = (timeInSeconds % 3600) / 60
        val seconds = timeInSeconds % 60
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    
    // 计时器效果
    LaunchedEffect(isTimerRunning, isPaused) {
        if (isTimerRunning && !isPaused) {
            timerStartTime = System.currentTimeMillis() / 1000 - savedElapsedTime
            while (isTimerRunning && !isPaused) {
                elapsedTime = System.currentTimeMillis() / 1000 - timerStartTime
                delay(1000)
            }
        }
    }
    
    // 手动输入时间对话框状态
    var showTimeInputDialog by remember { mutableStateOf(false) }
    var manualHours by remember { mutableStateOf("0") }
    var manualMinutes by remember { mutableStateOf("0") }
    var manualSeconds by remember { mutableStateOf("0") }
    
    // 保存数据到ViewModel的函数
    val saveDataToViewModel = { updatedShika: Shika ->
        // 确保时间戳使用当前时区
        val now = System.currentTimeMillis()
        val updatedShikaWithTimestamp = updatedShika.copy(
            timestamp = now,
            updatedAt = now
        )
        
        // 将更新后的数据保存到ViewModel
        viewModel.updateShika(updatedShikaWithTimestamp)
        android.util.Log.d("ShikaDetailScreen", "保存数据到ViewModel: ID=${updatedShikaWithTimestamp.id}, count=${updatedShikaWithTimestamp.count}, timestamp=${updatedShikaWithTimestamp.timestamp}")
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            // 多个按钮：手动输入、暂停/开始、结束计时
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 手动输入时间按钮
                FloatingActionButton(
                    onClick = { showTimeInputDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "手动输入时间"
                    )
                }
                
                // 暂停/继续按钮 (只在计时时显示)
                if (isTimerRunning) {
                    FloatingActionButton(
                        onClick = { 
                            isPaused = !isPaused
                            if (isPaused) {
                                // 暂停时保存当前时间
                                savedElapsedTime = elapsedTime
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        shape = CircleShape
                    ) {
                        Icon(
                            if (isPaused) 
                                Icons.Default.PlayArrow 
                            else 
                                Icons.Default.Pause,
                            contentDescription = if (isPaused) "继续计时" else "暂停计时"
                        )
                    }
                }
                
                // 开始/结束计时按钮
                FloatingActionButton(
                    onClick = { 
                        if (isTimerRunning) {
                            // 结束计时，记录时间
                            isTimerRunning = false
                            isPaused = false
                            
                            // 计算秒数并更新记录
                            val secondsElapsed = elapsedTime.toInt()
                            if (secondsElapsed > 0) {
                                // 保存计时结果（秒数）
                                timerResult = secondsElapsed
                                hasTimerCompleted = true
                                
                                // 将秒数转换为分钟（向上取整）用于count字段
                                val minutesElapsed = (secondsElapsed + 59) / 60 // 向上取整
                                
                                // 更新卡片数据
                                val updatedShika = shika.copy(
                                    count = minutesElapsed, // count字段仍然保存分钟数
                                    description = "计时完成：${formatTime(secondsElapsed.toLong())}",
                                    timestamp = System.currentTimeMillis()
                                )
                                shika = updatedShika
                                
                                // 保存到ViewModel
                                saveDataToViewModel(updatedShika)
                                
                                android.util.Log.d("ShikaDetailScreen", "计时结束，记录时间：${formatTime(secondsElapsed.toLong())}，hasTimerCompleted=$hasTimerCompleted")
                            } else {
                                // 即使时间为0，也标记为已完成计时
                                hasTimerCompleted = true
                                timerResult = 1 // 至少记录1秒
                                val updatedShika = shika.copy(
                                    count = 1, // 1分钟
                                    description = "计时完成：00:00:01",
                                    timestamp = System.currentTimeMillis()
                                )
                                shika = updatedShika
                                
                                // 保存到ViewModel
                                saveDataToViewModel(updatedShika)
                                
                                android.util.Log.d("ShikaDetailScreen", "计时结束，时间不足1秒，记录为1秒")
                            }
                            
                            // 重置计时器
                            savedElapsedTime = 0
                            elapsedTime = 0
                        } else {
                            // 开始计时
                            isTimerRunning = true
                            isPaused = false
                            savedElapsedTime = 0
                        }
                    },
                    containerColor = if (isTimerRunning) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(
                        if (isTimerRunning) 
                            Icons.Default.Close 
                        else 
                            Icons.Default.PlayArrow,
                        contentDescription = if (isTimerRunning) "结束计时" else "开始计时"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 自定义顶栏
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = "时间记录",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // 内容区域
            ShikaContent(
                shika = shika,
                isTimerRunning = isTimerRunning,
                isPaused = isPaused,
                elapsedTime = elapsedTime,
                timerResult = timerResult,
                hasTimerCompleted = hasTimerCompleted,
                formatTime = formatTime
            )
        }
    }
    
    // 手动输入时间对话框
    if (showTimeInputDialog) {
        AlertDialog(
            onDismissRequest = { showTimeInputDialog = false },
            title = { Text("手动设置时间") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("请输入时间（小时:分钟:秒）")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 小时输入
                        OutlinedTextField(
                            value = manualHours,
                            onValueChange = { 
                                if (it.isEmpty() || it.toIntOrNull() != null) {
                                    manualHours = it 
                                }
                            },
                            label = { Text("小时") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Text(":", modifier = Modifier.padding(horizontal = 4.dp))
                        
                        // 分钟输入
                        OutlinedTextField(
                            value = manualMinutes,
                            onValueChange = { 
                                if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() < 60)) {
                                    manualMinutes = it 
                                }
                            },
                            label = { Text("分钟") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Text(":", modifier = Modifier.padding(horizontal = 4.dp))
                        
                        // 秒钟输入
                        OutlinedTextField(
                            value = manualSeconds,
                            onValueChange = { 
                                if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() < 60)) {
                                    manualSeconds = it 
                                }
                            },
                            label = { Text("秒") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 计算总秒数
                        val hours = manualHours.toIntOrNull() ?: 0
                        val minutes = manualMinutes.toIntOrNull() ?: 0
                        val seconds = manualSeconds.toIntOrNull() ?: 0
                        
                        val totalSeconds = hours * 3600 + minutes * 60 + seconds
                        
                        if (totalSeconds > 0) {
                            // 直接更新记录，无需启动计时器
                            timerResult = totalSeconds // 保存秒数
                            hasTimerCompleted = true
                            
                            // 将秒数转换为分钟（向上取整）用于count字段
                            val totalMinutes = (totalSeconds + 59) / 60 // 向上取整
                            
                            // 更新卡片
                            val updatedShika = shika.copy(
                                count = totalMinutes, // count字段仍然保存分钟数
                                description = "手动设置：${formatTime(totalSeconds.toLong())}",
                                timestamp = System.currentTimeMillis()
                            )
                            shika = updatedShika
                            
                            // 保存到ViewModel
                            saveDataToViewModel(updatedShika)
                            
                            android.util.Log.d("ShikaDetailScreen", "手动设置时间：${formatTime(totalSeconds.toLong())}，hasTimerCompleted=$hasTimerCompleted")
                        } else {
                            // 即使时间为0，也设置为最小值1秒
                            timerResult = 1 // 1秒
                            hasTimerCompleted = true
                            
                            // 更新卡片
                            val updatedShika = shika.copy(
                                count = 1, // 1分钟
                                description = "手动设置：00:00:01",
                                timestamp = System.currentTimeMillis()
                            )
                            shika = updatedShika
                            
                            // 保存到ViewModel
                            saveDataToViewModel(updatedShika)
                            
                            android.util.Log.d("ShikaDetailScreen", "手动设置时间为0，改为设置为1秒")
                        }
                        
                        // 关闭对话框
                        showTimeInputDialog = false
                        
                        // 重置输入
                        manualHours = "0"
                        manualMinutes = "0"
                        manualSeconds = "0"
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeInputDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 在离开页面前保存数据
    DisposableEffect(Unit) {
        onDispose {
            // 如果计时器正在运行，先停止并保存当前时间
            if (isTimerRunning) {
                // 计算秒数
                val secondsElapsed = elapsedTime.toInt()
                if (secondsElapsed > 0) {
                    // 将秒数转换为分钟（向上取整）用于count字段
                    val minutesElapsed = (secondsElapsed + 59) / 60 // 向上取整
                    
                    // 更新卡片数据
                    val updatedShika = shika.copy(
                        count = minutesElapsed, // count字段仍然保存分钟数
                        description = "计时中断：${formatTime(secondsElapsed.toLong())}",
                        timestamp = System.currentTimeMillis()
                    )
                    shika = updatedShika
                }
            }
            
            // 无论是否有计时结果，都保存当前数据到ViewModel
            saveDataToViewModel(shika)
            android.util.Log.d("ShikaDetailScreen", "离开页面时保存数据: ID=${shika.id}, count=${shika.count}, description=${shika.description}")
        }
    }
}

@Composable
private fun ShikaContent(
    shika: Shika,
    isTimerRunning: Boolean = false,
    isPaused: Boolean = false,
    elapsedTime: Long = 0,
    timerResult: Int = 0,
    hasTimerCompleted: Boolean = false,
    formatTime: (Long) -> String = { "" }
) {
    // 调试日志
    LaunchedEffect(shika, timerResult, hasTimerCompleted) {
        android.util.Log.d("ShikaContent", "显示数据 - shika.count: ${shika.count}, timerResult: $timerResult, hasTimerCompleted: $hasTimerCompleted")
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 描述
        if (!shika.description.isNullOrEmpty()) {
            Text(
                text = shika.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 计时器卡片 - 始终显示
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
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 显示倒计时时间或设置的时间
                val displayText = when {
                    isTimerRunning -> formatTime(elapsedTime)
                    hasTimerCompleted -> {
                        // 直接使用保存的秒数格式化
                        val seconds = timerResult.toLong()
                        formatTime(seconds)
                    }
                    shika.count > 0 -> {
                        // 这里shika.count是分钟数，需要转换为秒
                        val seconds = shika.count * 60L
                        formatTime(seconds)
                    }
                    else -> "未计时"
                }
                
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                // 状态文本
                val statusText = when {
                    isTimerRunning && isPaused -> "已暂停"
                    isTimerRunning -> "正在计时"
                    hasTimerCompleted -> "记录时长"
                    shika.count > 0 -> "记录时长"
                    else -> "等待计时"
                }
                
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                // 记录时间
                if (!isTimerRunning && (hasTimerCompleted || shika.count > 0)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val recordTime = if (shika.timestamp > 0) {
                        val dateTime = Instant.ofEpochMilli(shika.timestamp)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                        dateTime.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"))
                    } else {
                        "未记录时间"
                    }
                    
                    Text(
                        text = "记录于 $recordTime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 操作提示
        val tipText = when {
            isTimerRunning && isPaused -> "已暂停计时，点击继续按钮恢复计时，或点击结束按钮完成计时"
            isTimerRunning -> "计时中，点击暂停按钮暂停计时，或点击结束按钮完成计时"
            else -> "点击右下角按钮开始计时或手动设置时间"
        }
        
        Text(
            text = tipText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isTimerRunning) 
                MaterialTheme.colorScheme.tertiary
            else 
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}