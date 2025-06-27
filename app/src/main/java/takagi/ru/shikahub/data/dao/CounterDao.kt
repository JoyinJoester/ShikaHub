package takagi.ru.shikahub.data.dao

import androidx.room.*
import takagi.ru.shikahub.data.entity.Shika
import java.time.LocalDate

@Dao
interface ShikaDao {
    @Query("SELECT * FROM shikas ORDER BY updatedAt DESC")
    suspend fun getAllShikas(): List<Shika>
    
    @Query("SELECT * FROM shikas WHERE id = :shikaId")
    suspend fun getShikaById(shikaId: Long): Shika
    
    @Insert
    suspend fun insertShika(shika: Shika): Long
    
    @Update
    suspend fun updateShika(shika: Shika)
    
    @Query("DELETE FROM shikas WHERE id = :shikaId")
    suspend fun deleteShika(shikaId: Long)
    
    @Query("UPDATE shikas SET count = count + 1, updatedAt = :timestamp, timestamp = :timestamp WHERE id = :shikaId")
    suspend fun incrementShika(shikaId: Long, timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM shikas WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getCountInTimeRange(startTime: Long, endTime: Long): Int
    
    @Query("SELECT SUM(count) FROM shikas")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM shikas WHERE timestamp >= :todayStart")
    suspend fun getTodayCount(todayStart: Long): Int
    
    @Query("SELECT COUNT(*) FROM shikas WHERE timestamp >= :weekStart")
    suspend fun getWeekCount(weekStart: Long): Int
} 