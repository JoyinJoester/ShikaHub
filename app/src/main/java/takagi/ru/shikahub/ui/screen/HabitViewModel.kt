package takagi.ru.shikahub.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import takagi.ru.shikahub.data.entity.Shika
import takagi.ru.shikahub.data.repository.ShikaRepository
import javax.inject.Inject
import java.time.LocalDateTime
import java.time.ZoneOffset

data class ShikaUiState(
    val shikas: List<Shika> = emptyList(),
    val selectedShika: Shika? = null,
    val todayCount: Int = 0,
    val weekCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class ShikaViewModel @Inject constructor(
    private val shikaRepository: ShikaRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ShikaUiState(isLoading = false))
    val uiState: StateFlow<ShikaUiState> = _uiState.asStateFlow()
    
    // 由于我们现在使用本地数据，不需要真正的加载操作
    fun refreshData() {
        android.util.Log.d("ShikaViewModel", "从数据库加载数据")
        viewModelScope.launch {
            try {
                val shikas = shikaRepository.getAllShikas()
                _uiState.value = _uiState.value.copy(
                    shikas = shikas,
                    isLoading = false
                )
                android.util.Log.d("ShikaViewModel", "成功加载${shikas.size}条记录")
            } catch (e: Exception) {
                android.util.Log.e("ShikaViewModel", "加载数据失败: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    // 设置临时数据
    fun setTempShika(shika: Shika) {
        android.util.Log.d("ShikaViewModel", "设置临时数据: ${shika.title}, ID: ${shika.id}")
        _uiState.value = _uiState.value.copy(
            selectedShika = shika
        )
    }
    
    // 这个方法不会真正访问数据库
    fun getShikaById(shikaId: Long) {
        android.util.Log.d("ShikaViewModel", "获取本地记录，ID: $shikaId")
        // 这里不做任何操作，因为我们现在使用本地数据
    }
    
    // 添加新记录（本地使用）
    fun addShika(title: String, description: String?) {
        android.util.Log.d("ShikaViewModel", "添加本地记录: $title")
        // 这里不做任何操作，因为我们现在使用本地数据
    }
    
    // 记录操作（本地使用）
    fun incrementShika(shikaId: Long) {
        android.util.Log.d("ShikaViewModel", "记录时间（本地模式）: $shikaId")
        // 这里不做任何操作，因为我们现在使用本地数据
    }
    
    // 更新卡片数据（启用数据库持久化）
    fun updateShika(shika: Shika) {
        android.util.Log.d("ShikaViewModel", "更新卡片数据: ID=${shika.id}, count=${shika.count}")
        
        // 获取当前卡片列表
        val currentShikas = _uiState.value.shikas.toMutableList()
        
        // 查找是否已存在该ID的卡片
        val existingIndex = currentShikas.indexOfFirst { it.id == shika.id }
        
        if (existingIndex >= 0) {
            // 更新已存在的卡片
            currentShikas[existingIndex] = shika
            android.util.Log.d("ShikaViewModel", "更新已存在卡片，索引: $existingIndex")
        } else {
            // 添加新卡片
            currentShikas.add(shika)
            android.util.Log.d("ShikaViewModel", "添加新卡片")
        }
        
        // 更新UI状态
        _uiState.value = _uiState.value.copy(
            shikas = currentShikas,
            selectedShika = shika
        )
        
        // 添加数据库持久化操作
        viewModelScope.launch {
            try {
                shikaRepository.updateShika(shika)
                android.util.Log.d("ShikaViewModel", "成功保存到数据库: ID=${shika.id}")
            } catch (e: Exception) {
                android.util.Log.e("ShikaViewModel", "保存到数据库失败: ${e.message}")
            }
        }
    }
    
    // 删除卡片（启用数据库持久化）
    fun deleteShika(shikaId: Long) {
        android.util.Log.d("ShikaViewModel", "删除卡片: ID=$shikaId")
        
        // 获取当前卡片列表
        val currentShikas = _uiState.value.shikas.toMutableList()
        
        // 移除指定ID的卡片
        val removed = currentShikas.removeIf { it.id == shikaId }
        
        if (removed) {
            android.util.Log.d("ShikaViewModel", "成功删除卡片")
            
            // 更新UI状态
            _uiState.value = _uiState.value.copy(
                shikas = currentShikas,
                // 如果删除的是当前选中的卡片，则清除选中状态
                selectedShika = if (_uiState.value.selectedShika?.id == shikaId) null else _uiState.value.selectedShika
            )
            
            // 添加数据库持久化操作
            viewModelScope.launch {
                try {
                    shikaRepository.deleteShika(shikaId)
                    android.util.Log.d("ShikaViewModel", "成功从数据库删除卡片: ID=$shikaId")
                } catch (e: Exception) {
                    android.util.Log.e("ShikaViewModel", "从数据库删除卡片失败: ${e.message}")
                }
            }
        } else {
            android.util.Log.d("ShikaViewModel", "未找到要删除的卡片")
        }
    }
    
    // 清空所有卡片（启用数据库持久化）
    fun clearAllShikas() {
        android.util.Log.d("ShikaViewModel", "清空所有卡片")
        
        // 获取当前所有卡片ID
        val shikaIds = _uiState.value.shikas.map { it.id }
        
        // 更新UI状态
        _uiState.value = _uiState.value.copy(
            shikas = emptyList(),
            selectedShika = null
        )
        
        // 添加数据库持久化操作
        viewModelScope.launch {
            try {
                // 逐个删除数据库中的卡片
                shikaIds.forEach { id ->
                    shikaRepository.deleteShika(id)
                }
                android.util.Log.d("ShikaViewModel", "成功从数据库清空所有卡片")
            } catch (e: Exception) {
                android.util.Log.e("ShikaViewModel", "从数据库清空卡片失败: ${e.message}")
            }
        }
    }

    // 添加新卡片（启用数据库持久化）
    fun addShika(shika: Shika) {
        android.util.Log.d("ShikaViewModel", "添加新卡片: ${shika.title}")
        
        viewModelScope.launch {
            try {
                // 将卡片保存到数据库
                val newId = shikaRepository.insertShika(shika)
                
                // 使用返回的ID更新卡片
                val updatedShika = shika.copy(id = newId)
                
                // 更新UI状态
                val currentShikas = _uiState.value.shikas.toMutableList()
                currentShikas.add(updatedShika)
                
                _uiState.value = _uiState.value.copy(
                    shikas = currentShikas,
                    selectedShika = updatedShika
                )
                
                android.util.Log.d("ShikaViewModel", "成功添加新卡片到数据库: ID=$newId")
            } catch (e: Exception) {
                android.util.Log.e("ShikaViewModel", "添加新卡片到数据库失败: ${e.message}")
            }
        }
    }

    // 增加卡片计数（启用数据库持久化）
    fun incrementShika(shikaId: Long, timestamp: Long) {
        android.util.Log.d("ShikaViewModel", "增加卡片计数: ID=$shikaId")
        
        viewModelScope.launch {
            try {
                // 在数据库中增加计数
                shikaRepository.incrementShika(shikaId, timestamp)
                
                // 更新UI状态
                val currentShikas = _uiState.value.shikas.toMutableList()
                val index = currentShikas.indexOfFirst { it.id == shikaId }
                
                if (index >= 0) {
                    // 获取更新后的卡片
                    val updatedShika = shikaRepository.getShikaById(shikaId)
                    
                    // 更新列表中的卡片
                    currentShikas[index] = updatedShika
                    
                    _uiState.value = _uiState.value.copy(
                        shikas = currentShikas,
                        selectedShika = if (_uiState.value.selectedShika?.id == shikaId) updatedShika else _uiState.value.selectedShika
                    )
                    
                    android.util.Log.d("ShikaViewModel", "成功增加卡片计数: ID=$shikaId, 新计数=${updatedShika.count}")
                }
            } catch (e: Exception) {
                android.util.Log.e("ShikaViewModel", "增加卡片计数失败: ${e.message}")
            }
        }
    }
}