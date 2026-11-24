package br.edu.ifsp.ifrota.poc_admin_entregas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.edu.ifsp.ifrota.poc_admin_entregas.data.local.dao.DeliveryDao
import br.edu.ifsp.ifrota.poc_admin_entregas.data.local.entity.DeliveryEntity

/**
 * Na PoC o banco só tem a tabela nova (`deliveries`). No app principal ela é a versão 4 do
 * banco, adicionada por `MIGRATION_3_4` sobre `drivers` e `vehicles`.
 */
@Database(entities = [DeliveryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deliveryDao(): DeliveryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "poc_admin_entregas_db"
                ).build().also { INSTANCE = it }
            }
    }
}
