package com.mercacerca.barriovivo.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TianguisEntity::class, TestimonioEntity::class, ReviewEntity::class],
    version = 2,
    exportSchema = true,
    // Room compara app/schemas/1.json con 2.json y genera la migración sola (aquí solo se agrega una tabla).
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tianguisDao(): TianguisDao
    abstract fun testimonioDao(): TestimonioDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        const val NOMBRE = "barrio_vivo.db"
    }
}
