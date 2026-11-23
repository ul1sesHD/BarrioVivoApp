package com.mercacerca.barriovivo.data.local

import androidx.room.ColumnInfo
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
    indices = [Index("tianguis_id")],
    foreignKeys = [
        ForeignKey(
            entity = TianguisEntity::class,
            parentColumns = ["id"],
            childColumns = ["tianguis_id"],
            onDelete = ForeignKey.CASCADE,
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
)
