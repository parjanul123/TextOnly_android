package text.only.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val itemName: String,
    val itemType: String,
    val resourceName: String,
    val acquiredDate: Long = System.currentTimeMillis()
)
