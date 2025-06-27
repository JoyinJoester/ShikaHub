package takagi.ru.shikahub.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import takagi.ru.shikahub.data.dao.ShikaDao
import takagi.ru.shikahub.data.entity.Shika
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZoneId
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

@Singleton
class ShikaRepository @Inject constructor(
    private val shikaDao: ShikaDao
) {
    // 添加缓存机制
    private val shikaCache = mutableMapOf<Long, Pair<Shika, Long>>() // <ID, <数据, 时间戳>>
    private val allShikasCache = Pair<List<Shika>, Long>(emptyList(), 0L) // <数据列表, 时间戳>
    private val cacheTimeoutMs = 30000L // 缓存30秒
    
    suspend fun getAllShikas(): List<Shika> = withContext(Dispatchers.IO) {
        // 检查缓存是否有效
        val currentTime = System.currentTimeMillis()
        if (allShikasCache.second > 0 && 
            currentTime - allShikasCache.second < cacheTimeoutMs &&
            allShikasCache.first.isNotEmpty()) {
            android.util.Log.d("ShikaRepository", "使用缓存获取所有卡片: ${allShikasCache.first.size} 个")
            return@withContext allShikasCache.first
        }
        
        try {
            android.util.Log.d("ShikaRepository", "从数据库获取所有卡片")
            val startTime = System.currentTimeMillis()
            val shikas = shikaDao.getAllShikas()
            val endTime = System.currentTimeMillis()
            
            android.util.Log.d("ShikaRepository", "获取所有卡片: ${shikas.size} 个，耗时: ${endTime - startTime}ms")
            
            // 更新缓存
            synchronized(allShikasCache) {
                val newCache = Pair(shikas, currentTime)
                allShikasCache::class.java.getDeclaredField("first").apply {
                    isAccessible = true
                    set(allShikasCache, newCache.first)
                }
                allShikasCache::class.java.getDeclaredField("second").apply {
                    isAccessible = true
                    set(allShikasCache, newCache.second)
                }
            }
            
            shikas
        } catch (e: Exception) {
            android.util.Log.e("ShikaRepository", "获取所有卡片出错: ${e.message}", e)
            if (allShikasCache.first.isNotEmpty()) {
                // 返回缓存数据
                android.util.Log.d("ShikaRepository", "出错时返回缓存数据")
                allShikasCache.first
            } else {
                // 无缓存数据，重新抛出异常
                throw e
            }
        }
    }
    
    suspend fun getShikaById(shikaId: Long): Shika = withContext(Dispatchers.IO) {
        // 检查缓存
        val currentTime = System.currentTimeMillis()
        shikaCache[shikaId]?.let { (shika, timestamp) ->
            if (currentTime - timestamp < cacheTimeoutMs) {
                android.util.Log.d("ShikaRepository", "使用缓存获取鹿，ID: $shikaId")
                return@withContext shika
            }
        }
        
        try {
            android.util.Log.d("ShikaRepository", "从数据库获取鹿，ID: $shikaId")
            val startTime = System.currentTimeMillis()
            val shika = shikaDao.getShikaById(shikaId)
            val endTime = System.currentTimeMillis()
            
            android.util.Log.d("ShikaRepository", "成功获取到鹿: ${shika.title}, ID: ${shika.id}，耗时: ${endTime - startTime}ms")
            
            // 更新缓存
            shikaCache[shikaId] = Pair(shika, currentTime)
            
            shika
        } catch (e: Exception) {
            android.util.Log.e("ShikaRepository", "获取鹿出错，ID: $shikaId, 错误: ${e.message}", e)
            // 如果有缓存则返回缓存
            shikaCache[shikaId]?.let {
                android.util.Log.d("ShikaRepository", "出错时返回缓存数据")
                return@withContext it.first
            }
            throw e
        }
    }
    
    suspend fun insertShika(shika: Shika): Long = withContext(Dispatchers.IO) {
        try {
            val id = shikaDao.insertShika(shika)
            
            // 清除所有缓存
            clearCache()
            
            id
        } catch (e: Exception) {
            android.util.Log.e("ShikaRepository", "插入鹿出错: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun updateShika(shika: Shika) = withContext(Dispatchers.IO) {
        try {
            shikaDao.updateShika(shika)
            
            // 更新缓存
            shikaCache[shika.id] = Pair(shika, System.currentTimeMillis())
            
            // 清除列表缓存
            synchronized(allShikasCache) {
                allShikasCache::class.java.getDeclaredField("second").apply {
                    isAccessible = true
                    set(allShikasCache, 0L) // 将时间戳设为0使缓存失效
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ShikaRepository", "更新鹿出错: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun deleteShika(shikaId: Long) = withContext(Dispatchers.IO) {
        try {
            shikaDao.deleteShika(shikaId)
            
            // 清除缓存
            shikaCache.remove(shikaId)
            clearCache()
        } catch (e: Exception) {
            android.util.Log.e("ShikaRepository", "删除鹿出错: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun incrementShika(shikaId: Long, timestamp: Long) = withContext(Dispatchers.IO) {
        try {
            shikaDao.incrementShika(shikaId, timestamp)
            
            // 清除该ID的缓存
            shikaCache.remove(shikaId)
            
            // 清除列表缓存
            synchronized(allShikasCache) {
                allShikasCache::class.java.getDeclaredField("second").apply {
                    isAccessible = true
                    set(allShikasCache, 0L) // 将时间戳设为0使缓存失效
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ShikaRepository", "增加鹿计数出错: ${e.message}", e)
            throw e
        }
    }
    
    private fun clearCache() {
        shikaCache.clear()
        synchronized(allShikasCache) {
            allShikasCache::class.java.getDeclaredField("first").apply {
                isAccessible = true
                set(allShikasCache, emptyList<Shika>())
            }
            allShikasCache::class.java.getDeclaredField("second").apply {
                isAccessible = true
                set(allShikasCache, 0L)
            }
        }
    }
    
    suspend fun getTodayCount(): Int = withContext(Dispatchers.IO) {
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        shikaDao.getTodayCount(todayStart)
    }
    
    suspend fun getWeekCount(): Int = withContext(Dispatchers.IO) {
        val weekFields = WeekFields.of(Locale.getDefault())
        val startOfWeek = LocalDate.now().with(weekFields.dayOfWeek(), 1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        shikaDao.getWeekCount(startOfWeek)
    }
    
    suspend fun getTotalCount(): Int = withContext(Dispatchers.IO) {
        shikaDao.getTotalCount() ?: 0
    }
}