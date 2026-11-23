package com.mercacerca.barriovivo

import com.mercacerca.barriovivo.core.Reloj
import com.mercacerca.barriovivo.core.geo.Geo
import com.mercacerca.barriovivo.core.geo.LatLon
import com.mercacerca.barriovivo.data.local.ViajeDao
import com.mercacerca.barriovivo.data.local.ViajeEntity
import com.mercacerca.barriovivo.data.repository.ViajeRepositoryImpl
import com.mercacerca.barriovivo.domain.model.Co2
import com.mercacerca.barriovivo.domain.model.Dia
import com.mercacerca.barriovivo.domain.model.ResultadoViaje
import com.mercacerca.barriovivo.domain.model.ResumenViajes
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.domain.model.Viaje
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViajesTest {

    private class FakeViajeDao : ViajeDao {
        val filas = MutableStateFlow<List<ViajeEntity>>(emptyList())
        override fun observeDeUsuario(usuarioId: Int) = filas.map { l -> l.filter { it.usuarioId == usuarioId }.sortedByDescending { it.fecha } }
        override suspend fun existeDesde(usuarioId: Int, tianguisId: Int, desde: Long) =
            filas.value.any { it.usuarioId == usuarioId && it.tianguisId == tianguisId && it.fecha >= desde }
        override suspend fun insertar(viaje: ViajeEntity): Long {
            val nuevo = viaje.copy(id = filas.value.size + 1)
            filas.value += nuevo
            return nuevo.id.toLong()
        }
        override suspend fun deUsuario(usuarioId: Int) = filas.value.filter { it.usuarioId == usuarioId }
        override suspend fun existeRemoto(remoteId: String) = filas.value.any { it.remoteId == remoteId }
        override suspend fun asignarRemoteId(id: Int, remoteId: String) {
            filas.value = filas.value.map { if (it.id == id) it.copy(remoteId = remoteId) else it }
        }
    }

    /** Tianguis a 1 km al norte del Zócalo. */
    private val tianguis = Tianguis(
        id = 107, dia = Dia.LUNES, nombre = "El de la esquina", ubicacion = "Calle 1", horaInicio = null, horaFin = null,
        coordenadas = LatLon(Geo.ZOCALO.lat + 1 / 111.2, Geo.ZOCALO.lon), nota = null,
    )

    @Test fun `co2 es km por el factor`() {
        assertEquals(0.17, Co2.ahorradoKg(1.0), 1e-9)
        assertEquals(0.425, Co2.ahorradoKg(2.5), 1e-9)
    }

    @Test fun `distancia es ida y vuelta`() {
        assertEquals(2.0, Co2.distanciaIdaYVueltaKm(Geo.ZOCALO, tianguis.coordenadas!!), 0.02)
    }

    @Test fun `resumen suma visitas, km y co2`() {
        val r = ResumenViajes(listOf(Viaje(1, 1, "A", 2.0, 0.34, 0), Viaje(2, 2, "B", 1.0, 0.17, 0)))
        assertEquals(2, r.visitas)
        assertEquals(3.0, r.totalKm, 1e-9)
        assertEquals(0.51, r.totalCo2Kg, 1e-9)
    }

    @Test fun `registrar guarda distancia, co2 y nombre del tianguis`() = runTest {
        val dao = FakeViajeDao()
        val r = ViajeRepositoryImpl(dao, Reloj { 1_000_000L }).registrar(usuarioId = 1, tianguis = tianguis, origen = Geo.ZOCALO)
        val v = (r as ResultadoViaje.Ok).viaje
        assertEquals(2.0, v.distanciaKm, 0.02)
        assertEquals(0.34, v.co2Kg, 0.01)
        assertEquals("El de la esquina", v.tituloTianguis)
        assertEquals(1, ViajeRepositoryImpl(dao, Reloj { 0 }).observeResumen(1).first().visitas)
    }

    @Test fun `solo una visita por tianguis al dia`() = runTest {
        val dao = FakeViajeDao()
        var ahora = Co2.inicioDelDia(1_763_942_400_000L) + 9 * 3_600_000L // 9:00
        val repo = ViajeRepositoryImpl(dao, Reloj { ahora })
        assertTrue(repo.registrar(1, tianguis, Geo.ZOCALO) is ResultadoViaje.Ok)
        ahora += 3 * 3_600_000L // 12:00, mismo día
        assertEquals(ResultadoViaje.YaRegistradoHoy, repo.registrar(1, tianguis, Geo.ZOCALO))
        ahora += 24 * 3_600_000L // al día siguiente sí
        assertTrue(repo.registrar(1, tianguis, Geo.ZOCALO) is ResultadoViaje.Ok)
    }

    @Test fun `mas de 10 km de ida no cuenta como caminando`() = runTest {
        val lejos = tianguis.copy(coordenadas = LatLon(Geo.ZOCALO.lat + 11 / 111.2, Geo.ZOCALO.lon))
        val r = ViajeRepositoryImpl(FakeViajeDao(), Reloj { 0 }).registrar(1, lejos, Geo.ZOCALO)
        assertTrue(r is ResultadoViaje.DemasiadoLejos)
    }

    @Test fun `sin coordenadas no se registra`() = runTest {
        val r = ViajeRepositoryImpl(FakeViajeDao(), Reloj { 0 }).registrar(1, tianguis.copy(coordenadas = null), Geo.ZOCALO)
        assertEquals(ResultadoViaje.SinCoordenadas, r)
    }
}
