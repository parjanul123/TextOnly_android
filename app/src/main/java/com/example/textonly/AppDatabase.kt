package text.only.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Server::class, ChannelEntity::class, MessageEntity::class, ConversationEntity::class, StoreItem::class, InventoryItem::class, TransactionLog::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun serverDao(): ServerDao
    abstract fun channelDao(): ChannelDao
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun storeDao(): StoreDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `store_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `price` INTEGER NOT NULL, `resourceName` TEXT NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `inventory` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `itemName` TEXT NOT NULL, `itemType` TEXT NOT NULL, `resourceName` TEXT NOT NULL, `acquiredDate` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `transaction_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `description` TEXT NOT NULL, `amount` INTEGER NOT NULL, `type` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `messages` ADD COLUMN `giftName` TEXT")
                database.execSQL("ALTER TABLE `messages` ADD COLUMN `giftValue` INTEGER")
                database.execSQL("ALTER TABLE `messages` ADD COLUMN `giftResource` TEXT")
            }
        }
        
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `messages` ADD COLUMN `filePriceUnit` TEXT")
                database.execSQL("ALTER TABLE `messages` ADD COLUMN `filePriceGiftName` TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "textonly_database"
                    )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}
