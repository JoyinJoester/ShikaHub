package takagi.ru.shikahub

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ShikaHubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 应用初始化配置
    }
} 