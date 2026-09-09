package br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.edu.ifsp.ifrota.data.local.dao.DriverDao
import br.edu.ifsp.ifrota.data.local.dao.VehicleDao
import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity

@Database(
    entities = [VehicleEntity::class, DriverEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao
    abstract fun driverDao(): DriverDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fleet_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
