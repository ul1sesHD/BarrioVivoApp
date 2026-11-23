package com.mercacerca.barriovivo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Todas las consultas usan parámetros (:dia): Room los enlaza, nunca concatena → sin inyección SQL.

@Dao
interface TianguisDao {
    @Query(
        "SELECT * FROM tianguis WHERE dia = :dia AND lat IS NOT NULL AND lon IS NOT NULL ORDER BY id"
    )
    fun observeConCoordenadasPorDia(dia: String): Flow<List<TianguisEntity>>

    @Query("SELECT * FROM tianguis WHERE lat IS NOT NULL AND lon IS NOT NULL")
    fun observeConCoordenadas(): Flow<List<TianguisEntity>>

    @Query("SELECT * FROM tianguis WHERE id = :id")
    fun observePorId(id: Int): Flow<TianguisEntity?>

    @Query("SELECT id FROM tianguis")
    suspend fun ids(): List<Int>

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

@Dao
interface ReviewDao {
    /** Con el color del ícono de quien la escribió (null en las reseñas de ejemplo). */
    @Query(
        "SELECT review.*, COALESCE(usuario.color_icono, review.color_autor) AS u_color FROM review " +
            "LEFT JOIN usuario ON usuario.id = review.usuario_id " +
            "WHERE review.tianguis_id = :tianguisId ORDER BY review.creado_en DESC, review.id DESC"
    )
    fun observePorTianguis(tianguisId: Int): Flow<List<ReviewConColor>>

    @Query("SELECT * FROM review WHERE tianguis_id = :tianguisId AND usuario_id = :usuarioId LIMIT 1")
    fun observeMia(tianguisId: Int, usuarioId: Int): Flow<ReviewEntity?>

    @Query("SELECT * FROM review WHERE tianguis_id = :tianguisId AND usuario_id = :usuarioId LIMIT 1")
    suspend fun mia(tianguisId: Int, usuarioId: Int): ReviewEntity?

    @Query(
        "SELECT review.*, tianguis.nombre AS t_nombre, tianguis.ubicacion AS t_ubicacion FROM review " +
            "INNER JOIN tianguis ON tianguis.id = review.tianguis_id " +
            "WHERE review.usuario_id = :usuarioId ORDER BY review.creado_en DESC"
    )
    fun observeDeUsuario(usuarioId: Int): Flow<List<ReviewConTianguis>>

    /** Inserta o reemplaza (mismo id = editar la evaluación). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(review: ReviewEntity): Long

    @Query("UPDATE review SET confianza = :confianza WHERE tianguis_id = :tianguisId AND usuario_id = :usuarioId")
    suspend fun actualizarConfianza(tianguisId: Int, usuarioId: Int, confianza: Boolean)

    @Query("SELECT * FROM review WHERE usuario_id = :usuarioId")
    suspend fun deUsuario(usuarioId: Int): List<ReviewEntity>

    @Query("SELECT * FROM review WHERE remote_id = :remoteId")
    suspend fun porRemoteId(remoteId: String): ReviewEntity?

    @Query("SELECT COUNT(*) FROM review")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ReviewEntity>)
}

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuario WHERE usuario = :usuario")
    suspend fun porUsuario(usuario: String): UsuarioEntity?

    /** Lanza SQLiteConstraintException si el usuario ya existe (índice UNIQUE). */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(usuario: UsuarioEntity): Long

    @Query("UPDATE usuario SET color_icono = :color WHERE id = :id")
    suspend fun cambiarColor(id: Int, color: String)

    @Query("SELECT usuario.* FROM usuario INNER JOIN sesion ON sesion.usuario_id = usuario.id WHERE sesion.id = 1")
    suspend fun sesionActual(): UsuarioEntity?

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)

    @Query("UPDATE usuario SET sync_inicial = 1 WHERE id = :id")
    suspend fun marcarSyncInicial(id: Int)

    @Query("SELECT COUNT(*) FROM usuario")
    suspend fun count(): Int

    @Query("SELECT usuario.* FROM usuario INNER JOIN sesion ON sesion.usuario_id = usuario.id WHERE sesion.id = 1")
    fun observeSesion(): Flow<UsuarioEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun abrirSesion(sesion: SesionEntity)

    @Query("DELETE FROM sesion")
    suspend fun cerrarSesion()
}

@Dao
interface FavoritoDao {
    @Query("SELECT EXISTS(SELECT 1 FROM favorito WHERE usuario_id = :usuarioId AND tianguis_id = :tianguisId)")
    fun observeEs(usuarioId: Int, tianguisId: Int): Flow<Boolean>

    @Query(
        "SELECT tianguis.* FROM tianguis INNER JOIN favorito ON favorito.tianguis_id = tianguis.id " +
            "WHERE favorito.usuario_id = :usuarioId ORDER BY favorito.creado_en DESC"
    )
    fun observeDeUsuario(usuarioId: Int): Flow<List<TianguisEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun agregar(favorito: FavoritoEntity)

    @Query("SELECT tianguis_id FROM favorito WHERE usuario_id = :usuarioId")
    suspend fun idsDeUsuario(usuarioId: Int): List<Int>

    @Query("DELETE FROM favorito WHERE usuario_id = :usuarioId AND tianguis_id = :tianguisId")
    suspend fun quitar(usuarioId: Int, tianguisId: Int)
}

@Dao
interface ViajeDao {
    @Query("SELECT * FROM viaje WHERE usuario_id = :usuarioId ORDER BY fecha DESC")
    fun observeDeUsuario(usuarioId: Int): Flow<List<ViajeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM viaje WHERE usuario_id = :usuarioId AND tianguis_id = :tianguisId AND fecha >= :desde)")
    suspend fun existeDesde(usuarioId: Int, tianguisId: Int, desde: Long): Boolean

    @Insert
    suspend fun insertar(viaje: ViajeEntity): Long

    @Query("SELECT * FROM viaje WHERE usuario_id = :usuarioId")
    suspend fun deUsuario(usuarioId: Int): List<ViajeEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM viaje WHERE remote_id = :remoteId)")
    suspend fun existeRemoto(remoteId: String): Boolean

    @Query("UPDATE viaje SET remote_id = :remoteId WHERE id = :id")
    suspend fun asignarRemoteId(id: Int, remoteId: String)
}
