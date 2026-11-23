package com.mercacerca.barriovivo

import com.mercacerca.barriovivo.data.local.TianguisDao
import com.mercacerca.barriovivo.data.local.TianguisEntity
import com.mercacerca.barriovivo.data.repository.TianguisRepositoryImpl
import com.mercacerca.barriovivo.domain.model.Dia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TianguisRepositoryTest {

    /** DAO falso en memoria: probamos el repositorio sin Room ni emulador. */
    private class FakeDao(private val filas: List<TianguisEntity>) : TianguisDao {
        var ultimoDia: String? = null
        override fun observeConCoordenadasPorDia(dia: String): Flow<List<TianguisEntity>> {
            ultimoDia = dia
            return flowOf(filas.filter { it.dia == dia && it.lat != null && it.lon != null })
        }
        override fun observePorId(id: Int): Flow<TianguisEntity?> = flowOf(filas.firstOrNull { it.id == id })
        override suspend fun count() = filas.size
        override suspend fun insertAll(items: List<TianguisEntity>) = Unit
    }

    private fun fila(id: Int, dia: String, nombre: String? = null, lat: Double? = 19.4) =
        TianguisEntity(id, dia, nombre, "Calle $id", "08:00", "17:00", lat, -99.1, null)

    @Test fun `consulta por el nombre del enum y mapea a dominio`() = runTest {
        val dao = FakeDao(listOf(fila(1, "SABADO", "El Chopo"), fila(2, "LUNES"), fila(3, "SABADO", lat = null)))
        val r = TianguisRepositoryImpl(dao).observeMapeablesPorDia(Dia.SABADO).first()

        assertEquals("SABADO", dao.ultimoDia)
        assertEquals(listOf(1), r.map { it.id })
        assertEquals(Dia.SABADO, r.single().dia)
        assertEquals("El Chopo", r.single().titulo)
    }

    @Test fun `sin nombre el titulo es la ubicacion`() = runTest {
        val r = TianguisRepositoryImpl(FakeDao(listOf(fila(2, "LUNES")))).observeMapeablesPorDia(Dia.LUNES).first()
        assertNull(r.single().nombre)
        assertEquals("Calle 2", r.single().titulo)
    }

    @Test fun `por id devuelve el tianguis o null`() = runTest {
        val repo = TianguisRepositoryImpl(FakeDao(listOf(fila(5, "MIERCOLES"))))
        assertEquals(Dia.MIERCOLES, repo.observePorId(5).first()?.dia)
        assertNull(repo.observePorId(999).first())
    }
}
