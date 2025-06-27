package takagi.ru.shikahub.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import takagi.ru.shikahub.data.dao.CounterDao
import takagi.ru.shikahub.data.entity.Counter
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDateTime
import java.time.ZoneOffset

@Singleton
class CounterRepository @Inject constructor(
    private val counterDao: CounterDao
) {
    suspend fun getAllCounters(): List<Counter> = withContext(Dispatchers.IO) {
        counterDao.getAllCounters()
    }
    
    suspend fun getCounterById(counterId: Long): Counter = withContext(Dispatchers.IO) {
        counterDao.getCounterById(counterId)
    }
    
    suspend fun insertCounter(counter: Counter): Long = withContext(Dispatchers.IO) {
        counterDao.insertCounter(counter)
    }
    
    suspend fun updateCounter(counter: Counter) = withContext(Dispatchers.IO) {
        counterDao.updateCounter(counter)
    }
    
    suspend fun deleteCounter(counterId: Long) = withContext(Dispatchers.IO) {
        counterDao.deleteCounter(counterId)
    }
    
    suspend fun incrementCounter(counterId: Long) = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000
        counterDao.incrementCounter(counterId, now)
    }
    
    suspend fun decrementCounter(counterId: Long) = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000
        counterDao.decrementCounter(counterId, now)
    }
    
    suspend fun resetCounter(counterId: Long) = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000
        counterDao.resetCounter(counterId, now)
    }
}
