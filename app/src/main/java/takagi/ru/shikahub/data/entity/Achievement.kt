package takagi.ru.shikahub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val icon: String, // 图标资源名称
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
