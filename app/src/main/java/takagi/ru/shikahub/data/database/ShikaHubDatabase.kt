package takagi.ru.shikahub.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import takagi.ru.shikahub.data.dao.ShikaDao
import takagi.ru.shikahub.data.entity.Shika

@Database(
    entities = [Shika::class],
    version = 1,
    exportSchema = false
)
abstract class ShikaHubDatabase : RoomDatabase() {
    abstract fun shikaDao(): ShikaDao
    
    companion object {
        @Volatile
        private var INSTANCE: ShikaHubDatabase? = null
        
        fun getDatabase(context: Context): ShikaHubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShikaHubDatabase::class.java,
                    "shikahub_database"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
