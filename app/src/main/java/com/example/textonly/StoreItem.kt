package text.only.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_items")
data class StoreItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val type: String, // "EMOTICON", "FRAME"
    val price: Int,
    val resourceName: String // E.g., "frame_neon_blue", "emote_smile"
)
