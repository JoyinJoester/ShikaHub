package takagi.ru.shikahub.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import takagi.ru.shikahub.data.dao.ShikaDao
import takagi.ru.shikahub.data.database.ShikaHubDatabase
import takagi.ru.shikahub.data.repository.ShikaRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShikaHubDatabase {
        return ShikaHubDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideShikaDao(database: ShikaHubDatabase): ShikaDao {
        return database.shikaDao()
    }
    
    @Provides
    @Singleton
    fun provideShikaRepository(shikaDao: ShikaDao): ShikaRepository {
        return ShikaRepository(shikaDao)
    }
}
