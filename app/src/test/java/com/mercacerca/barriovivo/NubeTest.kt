package com.mercacerca.barriovivo

import com.mercacerca.barriovivo.core.security.LimitadorIntentos
import com.mercacerca.barriovivo.core.security.PasswordHasher
import com.mercacerca.barriovivo.data.local.FavoritoEntity
import com.mercacerca.barriovivo.data.local.ReviewEntity
import com.mercacerca.barriovivo.data.local.TianguisEntity
import com.mercacerca.barriovivo.data.local.ViajeDao
import com.mercacerca.barriovivo.data.local.ViajeEntity
import com.mercacerca.barriovivo.data.remote.NubeRemota
import com.mercacerca.barriovivo.data.remote.PerfilDto
import com.mercacerca.barriovivo.data.remote.ResultadoNube
import com.mercacerca.barriovivo.data.remote.ReviewDto
import com.mercacerca.barriovivo.data.remote.ReviewPublicaDto
import com.mercacerca.barriovivo.data.remote.ViajeDto
import com.mercacerca.barriovivo.data.repository.AuthRepositoryImpl
import com.mercacerca.barriovivo.data.sync.SincronizadorNube
import com.mercacerca.barriovivo.domain.model.ErrorCuenta
import com.mercacerca.barriovivo.domain.model.ResultadoCuenta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Nube falsa en memoria: imita Supabase (cuentas, reseñas públicas, favoritos, viajes). */
class FakeNube(var hayInternet: Boolean = true) : NubeRemota {
    override val activa = true
    val cuentas = mutableMapOf<String, Pair<String, String>>() // usuario -> (password, uid)
    var sesion: String? = null
    val reviews = mutableListOf<ReviewPublicaDto>()
    val favoritos = mutableMapOf<String, Set<Int>>()
    val viajes = mutableListOf<ViajeDto>()

    private fun <T> red(bloque: () -> ResultadoNube<T>) = if (hayInternet) bloque() else ResultadoNube.SinConexion

    override suspend fun registrar(usuario: String, password: String, colorHex: String) = red {
        if (usuario in cuentas) ResultadoNube.UsuarioOcupado
        else "uid-$usuario".also { cuentas[usuario] = password to it; sesion = it }.let { ResultadoNube.Ok(it) }
    }
    override suspend fun iniciarSesion(usuario: String, password: String) = red {
        val c = cuentas[usuario]
        if (c == null || c.first != password) ResultadoNube.CredencialesInvalidas
        else { sesion = c.second; ResultadoNube.Ok(PerfilDto(c.second, usuario, "#5079B2")) }
    }
    override suspend fun cerrarSesion() { sesion = null }
    override suspend fun uidActual() = sesion
    override suspend fun cambiarColor(colorHex: String) = ResultadoNube.Ok(Unit)
    override suspend fun subirReviews(reviews: List<ReviewDto>) = red {
        reviews.forEach { r ->
            this.reviews.removeAll { it.userId == r.userId && it.tianguisId == r.tianguisId }
            this.reviews += ReviewPublicaDto("rev-${r.userId}-${r.tianguisId}", r.userId, r.tianguisId, r.variedad, r.tamano,
                r.calidad, r.confianza, r.comentario, "2026-11-23T10:00:00Z", r.userId.removePrefix("uid-"), "#D90368")
        }
        ResultadoNube.Ok(Unit)
    }
    override suspend fun bajarReviews() = red { ResultadoNube.Ok(reviews.toList()) }
    override suspend fun reemplazarFavoritos(tianguisIds: Set<Int>) = red { favoritos[sesion!!] = tianguisIds; ResultadoNube.Ok(Unit) }
    override suspend fun bajarFavoritos() = red { ResultadoNube.Ok(favoritos[sesion] ?: emptySet()) }
    override suspend fun subirViajes(viajes: List<ViajeDto>) = red {
        viajes.forEach { v -> this.viajes.removeAll { it.id == v.id }; this.viajes += v }
        ResultadoNube.Ok(Unit)
    }
    override suspend fun bajarViajes() = red { ResultadoNube.Ok(viajes.filter { it.userId == sesion }) }
}

class NubeTest {

    private class FakeViajeDao : ViajeDao {
        val filas = mutableListOf<ViajeEntity>()
        override fun observeDeUsuario(usuarioId: Int): Flow<List<ViajeEntity>> = emptyFlow()
        override suspend fun existeDesde(usuarioId: Int, tianguisId: Int, desde: Long) = false
        override suspend fun insertar(viaje: ViajeEntity): Long { filas += viaje.copy(id = filas.size + 1); return filas.size.toLong() }
        override suspend fun deUsuario(usuarioId: Int) = filas.filter { it.usuarioId == usuarioId }
        override suspend fun existeRemoto(remoteId: String) = filas.any { it.remoteId == remoteId }
        override suspend fun asignarRemoteId(id: Int, remoteId: String) { filas.replaceAll { if (it.id == id) it.copy(remoteId = remoteId) else it } }
    }

    /** Un teléfono: su Room falso + su repositorio de cuentas + su sincronizador, contra la nube compartida. */
    private inner class Telefono(nube: FakeNube, scope: TestScope) {
        val usuarios = AuthRepositoryTest.FakeUsuarioDao()
        val reviews = EvaluarTest.FakeReviewDao()
        val favoritos = EvaluarTest.FakeFavoritoDao()
        val viajes = FakeViajeDao()
        private val catalogo = TianguisRepositoryTest.FakeDao(listOf(90, 107, 505).map {
            TianguisEntity(it, "LUNES", null, "Calle $it", null, null, 19.4, -99.1, null)
        })
        val sync = SincronizadorNube(nube, usuarios, reviews, favoritos, viajes, catalogo, scope)
        val auth = AuthRepositoryImpl(
            usuarios, PasswordHasher(iteraciones = 1_000), LimitadorIntentos(),
            StandardTestDispatcher(scope.testScheduler), nube, sync = {},
        )
    }

    @Test fun `registrar con nube crea la cuenta alla y la liga a la local`() = runTest {
        val nube = FakeNube()
        val tel = Telefono(nube, this)
        assertEquals(ResultadoCuenta.Ok, tel.auth.registrar("marce", "mercado123", 0))
        assertEquals("uid-marce", tel.usuarios.usuarios.single().remoteId)
        assertTrue("marce" in nube.cuentas)
    }

    @Test fun `el nombre debe ser unico en toda la comunidad`() = runTest {
        val nube = FakeNube().apply { cuentas["marce"] = "otra12345" to "uid-marce" }
        assertEquals(ResultadoCuenta.Error(ErrorCuenta.USUARIO_OCUPADO), Telefono(nube, this).auth.registrar("marce", "mercado123", 0))
    }

    @Test fun `registrar sin internet avisa y no crea nada`() = runTest {
        val tel = Telefono(FakeNube(hayInternet = false), this)
        assertEquals(ResultadoCuenta.Error(ErrorCuenta.SIN_CONEXION), tel.auth.registrar("marce", "mercado123", 0))
        assertTrue(tel.usuarios.usuarios.isEmpty())
    }

    @Test fun `sin internet se puede entrar con la copia local`() = runTest {
        val nube = FakeNube()
        val tel = Telefono(nube, this)
        tel.auth.registrar("marce", "mercado123", 0)
        tel.auth.cerrarSesion()
        nube.hayInternet = false
        assertEquals(ResultadoCuenta.Ok, tel.auth.iniciarSesion("marce", "mercado123"))
        assertEquals(ResultadoCuenta.Error(ErrorCuenta.CREDENCIALES_INCORRECTAS), run {
            tel.auth.cerrarSesion(); tel.auth.iniciarSesion("marce", "equivocada")
        })
    }

    @Test fun `lo que haces en un telefono aparece en otro`() = runTest {
        val nube = FakeNube()
        val a = Telefono(nube, this)
        a.auth.registrar("marce", "mercado123", 0)
        val yo = a.usuarios.usuarios.single()
        a.reviews.guardar(ReviewEntity(tianguisId = 90, autor = "marce", variedad = 5, tamano = 4, calidad = 5,
            confianza = true, comentario = "¡Bien!", usuarioId = yo.id))
        a.favoritos.agregar(FavoritoEntity(yo.id, 505))
        a.viajes.insertar(ViajeEntity(usuarioId = yo.id, tianguisId = 107, tituloTianguis = "El 107", distanciaKm = 2.0, co2Kg = 0.34, fecha = 0))
        assertTrue(a.sync.sincronizar())
        assertNotNull(a.viajes.filas.single().remoteId) // viaje viejo sin uuid: se le asignó uno

        // Teléfono nuevo, mismo usuario: al entrar baja todo.
        val b = Telefono(nube, this)
        assertEquals(ResultadoCuenta.Ok, b.auth.iniciarSesion("marce", "mercado123"))
        assertTrue(b.sync.sincronizar())
        val yoEnB = b.usuarios.usuarios.single()
        assertEquals(5, b.reviews.filas.single { it.usuarioId == yoEnB.id }.variedad)
        assertEquals(listOf(505), b.favoritos.idsDeUsuario(yoEnB.id))
        assertEquals("El 107", b.viajes.filas.single().tituloTianguis)
        assertTrue(yoEnB.syncInicial || b.usuarios.usuarios.single().syncInicial)
    }

    @Test fun `las resenas de otros se ven aunque no tengas sesion`() = runTest {
        val nube = FakeNube()
        val a = Telefono(nube, this)
        a.auth.registrar("sofia", "mercado123", 0)
        a.reviews.guardar(ReviewEntity(tianguisId = 90, autor = "sofia", variedad = 4, tamano = 4, calidad = 4,
            confianza = false, comentario = null, usuarioId = a.usuarios.usuarios.single().id))
        a.sync.sincronizar()

        val anonimo = Telefono(nube, this)
        nube.sesion = null
        anonimo.sync.sincronizar()
        val r = anonimo.reviews.filas.single()
        assertEquals("sofia", r.autor)
        assertEquals("#D90368", r.colorAutor)
        assertEquals(null, r.usuarioId) // es de otra persona: no la puedes editar
    }
}
