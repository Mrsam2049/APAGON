package com.iue.apagon.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.iue.apagon.data.local.dao.EnergiaDao
import com.iue.apagon.data.local.dao.LogroDao
import com.iue.apagon.data.local.dao.PartidaDao
import com.iue.apagon.data.local.dao.PerfilDao
import com.iue.apagon.data.local.entity.EnergiaDiariaEntity
import com.iue.apagon.data.local.entity.LogroEntity
import com.iue.apagon.data.local.entity.NocheEntity
import com.iue.apagon.data.local.entity.PartidaEntity
import com.iue.apagon.data.local.entity.PerfilEntity

@Database(
    entities = [
        PartidaEntity::class,
        NocheEntity::class,
        EnergiaDiariaEntity::class,
        PerfilEntity::class,
        LogroEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun partidaDao(): PartidaDao
    abstract fun energiaDao(): EnergiaDao
    abstract fun perfilDao(): PerfilDao
    abstract fun logroDao(): LogroDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private const val SEED_PERFIL =
            "INSERT OR IGNORE INTO `perfil` " +
                "(`id`,`vatiosTotales`,`cartasDesbloqueadas`,`municipiosDesbloqueados`,`mejorasCompradas`) " +
                "VALUES (1,0,'','apartado','')"

        /** v1 → v2: agrega la tabla de perfil sin tocar partidas/noches (historial intacto). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `perfil` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`vatiosTotales` INTEGER NOT NULL, " +
                        "`cartasDesbloqueadas` TEXT NOT NULL, " +
                        "`municipiosDesbloqueados` TEXT NOT NULL, " +
                        "`mejorasCompradas` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL(SEED_PERFIL)
            }
        }

        /** v2 → v3: agrega la tabla de logros (las filas se siembran desde el repositorio). */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `logros` (" +
                        "`id` TEXT NOT NULL, " +
                        "`desbloqueado` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        /** Siembra el perfil por defecto en instalaciones nuevas (DB creada directo en la última versión). */
        private val SEED_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(SEED_PERFIL)
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "apagon.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(SEED_CALLBACK)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
