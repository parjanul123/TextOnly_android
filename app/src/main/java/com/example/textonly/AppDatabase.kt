package text.only.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Server::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun serverDao(): ServerDao

    companion object {
        // Volatile asigură că valoarea este mereu actualizată pentru toate thread-urile
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // Folosim `synchronized` pentru a ne asigura că nu creăm două instanțe
            // ale bazei de date în același timp din thread-uri diferite.
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "textonly_database"
                    ).build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}
