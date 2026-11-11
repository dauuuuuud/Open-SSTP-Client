package kittoku.osc.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(
    entities = [ServerEntity::class, CustomServerEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun customServerDao(): CustomServerDao

    companion object {
        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "sstp_vpn.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
