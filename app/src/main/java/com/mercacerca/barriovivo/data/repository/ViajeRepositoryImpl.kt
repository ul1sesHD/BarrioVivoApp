package com.mercacerca.barriovivo.data.repository

import com.mercacerca.barriovivo.core.Reloj
import com.mercacerca.barriovivo.core.geo.LatLon
import com.mercacerca.barriovivo.data.local.ViajeDao
import com.mercacerca.barriovivo.data.local.ViajeEntity
import com.mercacerca.barriovivo.data.sync.SolicitudSync
import com.mercacerca.barriovivo.domain.model.Co2
import com.mercacerca.barriovivo.domain.model.ResultadoViaje
import com.mercacerca.barriovivo.domain.model.ResumenViajes
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.domain.model.Viaje
import com.mercacerca.barriovivo.domain.repository.ViajeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class ViajeRepositoryImpl @Inject constructor(
    private val dao: ViajeDao,
    private val reloj: Reloj,
    private val sync: SolicitudSync = SolicitudSync {},
) : ViajeRepository {

    override fun observeResumen(usuarioId: Int): Flow<ResumenViajes> =
        dao.observeDeUsuario(usuarioId).map { lista -> ResumenViajes(lista.map { it.toDomain() }) }

    override suspend fun registrar(usuarioId: Int, tianguis: Tianguis, origen: LatLon): ResultadoViaje {
        val destino = tianguis.coordenadas ?: return ResultadoViaje.SinCoordenadas
        val ahora = reloj.ahora()
        if (dao.existeDesde(usuarioId, tianguis.id, Co2.inicioDelDia(ahora))) return ResultadoViaje.YaRegistradoHoy

        val km = Co2.distanciaIdaYVueltaKm(origen, destino)
        if (km > Co2.MAX_KM_IDA_Y_VUELTA) return ResultadoViaje.DemasiadoLejos(km)
        val entidad = ViajeEntity(
            usuarioId = usuarioId,
            tianguisId = tianguis.id,
            tituloTianguis = tianguis.titulo,
            distanciaKm = km,
            co2Kg = Co2.ahorradoKg(km),
            fecha = ahora,
            remoteId = UUID.randomUUID().toString(),
        )
        val id = dao.insertar(entidad)
        sync.solicitar()
        return ResultadoViaje.Ok(entidad.copy(id = id.toInt()).toDomain())
    }
}

private fun ViajeEntity.toDomain() = Viaje(id, tianguisId, tituloTianguis, distanciaKm, co2Kg, fecha)
