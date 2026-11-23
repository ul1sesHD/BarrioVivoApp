package com.mercacerca.barriovivo.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Las tablas replican data/04_seed_barriovivo.sql (mismos nombres de columna).

@Entity(tableName = "tianguis", indices = [Index("dia")])
data class TianguisEntity(
    @PrimaryKey val id: Int,
    /** Normalizado al nombre del enum Dia: LUNES..DOMINGO (sin acentos). */
    val dia: String,
    val nombre: String?,
    val ubicacion: String,
    @ColumnInfo(name = "hora_ini") val horaIni: String?,
    @ColumnInfo(name = "hora_fin") val horaFin: String?,
    val lat: Double?,
    val lon: Double?,
    val nota: String?,
)

@Entity(tableName = "testimonio")
data class TestimonioEntity(
    @PrimaryKey val id: Int,
    val autor: String,
    val texto: String,
)

/** Evaluación: 3 preguntas de 1 a 5 + "tianguis de confianza". */
@Entity(
    tableName = "review",
    indices = [
        Index("tianguis_id"),
        // Una evaluación por persona y tianguis (las seed tienen usuario_id NULL y no chocan).
        Index(value = ["usuario_id", "tianguis_id"], unique = true),
        Index(value = ["remote_id"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = TianguisEntity::class,
            parentColumns = ["id"],
            childColumns = ["tianguis_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuario_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "tianguis_id") val tianguisId: Int,
    val autor: String,
    val variedad: Int,
    val tamano: Int,
    val calidad: Int,
    val confianza: Boolean,
    val comentario: String?,
    /** Epoch en milisegundos (en Postgres será timestamptz). */
    @ColumnInfo(name = "creado_en") val creadoEn: Long = System.currentTimeMillis(),
    /** Quién la escribió; null en las reseñas de ejemplo. */
    @ColumnInfo(name = "usuario_id") val usuarioId: Int? = null,
    /** uuid en Supabase; null = solo local (seed o aún no sincronizada de otra persona). */
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    /** Color del autor cuando la reseña viene de otra persona en la nube. */
    @ColumnInfo(name = "color_autor") val colorAutor: String? = null,
)

data class ReviewConColor(
    @Embedded val review: ReviewEntity,
    @ColumnInfo(name = "u_color") val colorIcono: String?,
)

/** Reseña + nombre del tianguis, para "Mis reseñas" en el perfil. */
data class ReviewConTianguis(
    @Embedded val review: ReviewEntity,
    @ColumnInfo(name = "t_nombre") val nombre: String?,
    @ColumnInfo(name = "t_ubicacion") val ubicacion: String,
)

/** Usuario local. De la contraseña solo se guarda hash + salt + iteraciones (PBKDF2). */
@Entity(tableName = "usuario", indices = [Index(value = ["usuario"], unique = true)])
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val usuario: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "password_salt") val passwordSalt: String,
    @ColumnInfo(name = "password_iter") val passwordIter: Int,
    /** "#D90368" */
    @ColumnInfo(name = "color_icono") val colorIcono: String,
    @ColumnInfo(name = "creado_en") val creadoEn: Long = System.currentTimeMillis(),
    /** uid de Supabase Auth; null = cuenta solo en este teléfono. */
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    /** True cuando ya se bajaron sus datos de la nube a este teléfono (primer login). */
    @ColumnInfo(name = "sync_inicial", defaultValue = "0") val syncInicial: Boolean = false,
)

/** Sesión abierta: a lo más un renglón (id = 1). Borrarlo = cerrar sesión. */
@Entity(
    tableName = "sesion",
    indices = [Index("usuario_id")],
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuario_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SesionEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "usuario_id") val usuarioId: Int,
)

/** "Mis tianguis de confianza": uno por (usuario, tianguis). */
@Entity(
    tableName = "favorito",
    primaryKeys = ["usuario_id", "tianguis_id"],
    indices = [Index("tianguis_id")],
    foreignKeys = [
        ForeignKey(entity = UsuarioEntity::class, parentColumns = ["id"], childColumns = ["usuario_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TianguisEntity::class, parentColumns = ["id"], childColumns = ["tianguis_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class FavoritoEntity(
    @ColumnInfo(name = "usuario_id") val usuarioId: Int,
    @ColumnInfo(name = "tianguis_id") val tianguisId: Int,
    @ColumnInfo(name = "creado_en") val creadoEn: Long = System.currentTimeMillis(),
)

/**
 * "Tus viajes": cada ida caminando/en bici a un tianguis.
 * Se guarda el nombre del tianguis tal cual para que el ticket no cambie aunque cambie el catálogo.
 */
@Entity(
    tableName = "viaje",
    indices = [Index("usuario_id"), Index("tianguis_id"), Index(value = ["remote_id"], unique = true)],
    foreignKeys = [
        ForeignKey(entity = UsuarioEntity::class, parentColumns = ["id"], childColumns = ["usuario_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TianguisEntity::class, parentColumns = ["id"], childColumns = ["tianguis_id"], onDelete = ForeignKey.SET_NULL),
    ],
)
data class ViajeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "usuario_id") val usuarioId: Int,
    @ColumnInfo(name = "tianguis_id") val tianguisId: Int?,
    @ColumnInfo(name = "tianguis_titulo") val tituloTianguis: String,
    /** Ida y vuelta. */
    @ColumnInfo(name = "distancia_km") val distanciaKm: Double,
    /** distancia_km × Co2.KG_POR_KM (se guarda con el factor que había ese día). */
    @ColumnInfo(name = "co2_kg") val co2Kg: Double,
    val fecha: Long,
    /** uuid con el que se sube a la nube (sin duplicar aunque se reintente). */
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
)
