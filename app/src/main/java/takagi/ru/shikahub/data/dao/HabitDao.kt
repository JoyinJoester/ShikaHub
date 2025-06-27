package takagi.ru.shikahub.data.dao

import androidx.room.*
import takagi.ru.shikahub.data.entity.Counter

@Dao
interface CounterDao {
    @Query("SELECT * FROM counters ORDER BY updatedAt DESC")
    suspend fun getAllCounters(): List<Counter>
    
    @Query("SELECT * FROM counters WHERE id = :counterId")
    suspend fun getCounterById(counterId: Long): Counter
    
    @Insert
    suspend fun insertCounter(counter: Counter): Long
    
    @Update
    suspend fun updateCounter(counter: Counter)
    
    @Query("DELETE FROM counters WHERE id = :counterId")
    suspend fun deleteCounter(counterId: Long)
    
    @Query("UPDATE counters SET count = count + 1, updatedAt = :timestamp WHERE id = :counterId")
    suspend fun incrementCounter(counterId: Long, timestamp: Long)
    
    @Query("UPDATE counters SET count = CASE WHEN count > 0 THEN count - 1 ELSE 0 END, updatedAt = :timestamp WHERE id = :counterId")
    suspend fun decrementCounter(counterId: Long, timestamp: Long)
    
    @Query("UPDATE counters SET count = 0, updatedAt = :timestamp WHERE id = :counterId")
    suspend fun resetCounter(counterId: Long, timestamp: Long)
}
