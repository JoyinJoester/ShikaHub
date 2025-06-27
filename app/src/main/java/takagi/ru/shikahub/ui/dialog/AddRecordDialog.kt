package takagi.ru.shikahub.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordDialog(
    onDismiss: () -> Unit,
    onSave: (duration: Int?, rating: Int?, notes: String?) -> Unit
) {
    var durationText by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(3) } // 默认3星
    var notes by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题
                Text(
                    text = "添加记录",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // 持续时间
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { 
                        // 只允许输入数字
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            durationText = it 
                        }
                    },
                    label = { Text("持续时间（分钟）") },
                    placeholder = { Text("例如：30") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                // 评分
                Column {
                    Text(
                        text = "评分：$rating",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Slider(
                        value = rating.toFloat(),
                        onValueChange = { rating = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1", style = MaterialTheme.typography.bodySmall)
                        Text("5", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                // 备注
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注（可选）") },
                    placeholder = { Text("今天的感受...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val duration = durationText.toIntOrNull()
                            onSave(duration, rating, notes.ifBlank { null })
                        }
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}
