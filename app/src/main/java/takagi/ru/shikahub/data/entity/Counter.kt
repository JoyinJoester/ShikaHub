package takagi.ru.shikahub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shikas")
data class Shika(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,     // 行为标题
    val description: String? = null,  // 行为描述
    val count: Int = 0,     // 当前计数
    val createdAt: Long,    // 创建时间
    val updatedAt: Long,    // 最后更新时间
    val timestamp: Long     // 最后一次记录的时间戳
) 