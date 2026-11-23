package com.mercacerca.barriovivo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TianguisEntity::class, TestimonioEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tianguisDao(): TianguisDao
    abstract fun testimonioDao(): TestimonioDao

    companion object {
        const val NOMBRE = "barrio_vivo.db"
    }
}
