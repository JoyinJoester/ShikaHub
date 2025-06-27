package takagi.ru.shikahub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counters")
data class Counter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val count: Int = 0,     // 当前计数
    val goal: Int? = null,  // 目标次数，可选
    val createdAt: Long,
    val updatedAt: Long
)
