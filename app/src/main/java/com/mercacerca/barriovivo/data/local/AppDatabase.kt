package com.mercacerca.barriovivo.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TianguisEntity::class, TestimonioEntity::class, ReviewEntity::class,
        UsuarioEntity::class, SesionEntity::class, FavoritoEntity::class, ViajeEntity::class,
    ],
    version = 6,
    exportSchema = true,
    // Room compara los esquemas exportados (app/schemas) y genera cada migración sola.
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4), // review.usuario_id + tabla favorito
        AutoMigration(from = 4, to = 5), // tabla viaje
        AutoMigration(from = 5, to = 6), // ids de Supabase (remote_id) para sincronizar
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tianguisDao(): TianguisDao
    abstract fun testimonioDao(): TestimonioDao
    abstract fun reviewDao(): ReviewDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun favoritoDao(): FavoritoDao
    abstract fun viajeDao(): ViajeDao

    companion object {
        const val NOMBRE = "barrio_vivo.db"
    }
}
