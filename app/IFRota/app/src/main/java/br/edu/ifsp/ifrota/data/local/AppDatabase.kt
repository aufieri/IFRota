package br.edu.ifsp.ifrota.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.edu.ifsp.ifrota.data.local.dao.DriverDao
import br.edu.ifsp.ifrota.data.local.dao.VehicleDao
import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity

@Database(
    entities = [VehicleEntity::class, DriverEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao
    abstract fun driverDao(): DriverDao

    companion object {
        private fun SupportSQLiteDatabase.columnNames(table: String): Set<String> =
            query("PRAGMA table_info($table)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(nameIndex))
                }
            }

        private fun SupportSQLiteDatabase.addColumnIfMissing(
            table: String,
            existing: Set<String>,
            column: String,
            definition: String
        ) {
            if (column !in existing) {
                execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
            }
        }

        private fun SupportSQLiteDatabase.upgradeToV3() {
            val vehicleColumns = columnNames("vehicles")
            addColumnIfMissing("vehicles", vehicleColumns, "ownerId", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing("vehicles", vehicleColumns, "isSynced", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing("vehicles", vehicleColumns, "isDeleted", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing("vehicles", vehicleColumns, "updatedAt", "INTEGER NOT NULL DEFAULT 0")
            execSQL("CREATE INDEX IF NOT EXISTS index_vehicles_ownerId ON vehicles(ownerId)")

            val driverColumns = columnNames("drivers")
            addColumnIfMissing("drivers", driverColumns, "email", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing("drivers", driverColumns, "cnh", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing("drivers", driverColumns, "phone", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing("drivers", driverColumns, "isSynced", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing("drivers", driverColumns, "isDeleted", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing("drivers", driverColumns, "updatedAt", "INTEGER NOT NULL DEFAULT 0")
        }

        private val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) = db.upgradeToV3()
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) = db.upgradeToV3()
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fleet_database"
                )
                    .addMigrations(MIGRATION_1_3, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
