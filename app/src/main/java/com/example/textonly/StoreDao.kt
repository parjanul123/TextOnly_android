package text.only.app

import androidx.room.Dao
import androidx.room.Delete
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

    @Delete
    suspend fun deleteInventoryItem(item: InventoryItem)

    @Query("SELECT * FROM inventory")
    suspend fun getInventory(): List<InventoryItem>
    
    // --- NEW: Clear Inventory ---
    @Query("DELETE FROM inventory")
    suspend fun clearInventory()

    // Transaction methods
    @Insert
    suspend fun insertTransaction(log: TransactionLog)

    @Query("SELECT * FROM transaction_logs ORDER BY timestamp DESC")
    suspend fun getTransactions(): List<TransactionLog>
}
