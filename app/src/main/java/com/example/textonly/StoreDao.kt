package text.only.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface StoreDao {
    @Insert
    suspend fun insertItem(item: StoreItem)

    @Query("SELECT * FROM store_items")
    suspend fun getAllItems(): List<StoreItem>
    
    // Inventory methods
    @Insert
    suspend fun insertInventoryItem(item: InventoryItem)

    @Query("SELECT * FROM inventory")
    suspend fun getInventory(): List<InventoryItem>

    // Transaction methods
    @Insert
    suspend fun insertTransaction(log: TransactionLog)

    @Query("SELECT * FROM transaction_logs ORDER BY timestamp DESC")
    suspend fun getTransactions(): List<TransactionLog>
}
