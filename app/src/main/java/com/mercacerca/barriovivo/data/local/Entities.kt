package com.mercacerca.barriovivo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
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
