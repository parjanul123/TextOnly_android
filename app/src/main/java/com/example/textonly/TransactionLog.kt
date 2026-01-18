package text.only.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_logs")
data class TransactionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    val amount: Int, // Can be negative for spent, positive for gained
    val type: String, // "PURCHASE", "GIFT_SENT", "GIFT_RECEIVED"
    val timestamp: Long = System.currentTimeMillis()
)
