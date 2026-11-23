package com.mercacerca.barriovivo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Todas las consultas usan parámetros (:dia): Room los enlaza, nunca concatena → sin inyección SQL.

@Dao
interface TianguisDao {
    @Query(
        "SELECT * FROM tianguis WHERE dia = :dia AND lat IS NOT NULL AND lon IS NOT NULL ORDER BY id"
    )
    fun observeConCoordenadasPorDia(dia: String): Flow<List<TianguisEntity>>

    @Query("SELECT COUNT(*) FROM tianguis")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TianguisEntity>)
}

@Dao
interface TestimonioDao {
    @Query("SELECT * FROM testimonio ORDER BY id")
    fun observeTodos(): Flow<List<TestimonioEntity>>

    @Query("SELECT COUNT(*) FROM testimonio")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TestimonioEntity>)
}
