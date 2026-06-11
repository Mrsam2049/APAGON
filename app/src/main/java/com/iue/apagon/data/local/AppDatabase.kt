package com.iue.apagon.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iue.apagon.data.local.dao.EnergiaDao
import com.iue.apagon.data.local.dao.PartidaDao
import com.iue.apagon.data.local.entity.EnergiaDiariaEntity
import com.iue.apagon.data.local.entity.NocheEntity
import com.iue.apagon.data.local.entity.PartidaEntity

@Database(
    entities = [
        PartidaEntity::class,
        NocheEntity::class,
        EnergiaDiariaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun partidaDao(): PartidaDao
    abstract fun energiaDao(): EnergiaDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "apagon.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
