package com.mercacerca.barriovivo

import com.mercacerca.barriovivo.core.security.LimitadorIntentos
import com.mercacerca.barriovivo.core.security.PasswordHasher
import com.mercacerca.barriovivo.data.local.SesionEntity
import com.mercacerca.barriovivo.data.local.UsuarioDao
import com.mercacerca.barriovivo.data.local.UsuarioEntity
import com.mercacerca.barriovivo.data.repository.AuthRepositoryImpl
import com.mercacerca.barriovivo.domain.model.ErrorCuenta
import com.mercacerca.barriovivo.domain.model.ResultadoCuenta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AuthRepositoryTest {

    /** DAO en memoria que imita a Room (incluida la sesión). */
    class FakeUsuarioDao : UsuarioDao {
        val usuarios = mutableListOf<UsuarioEntity>()
        private val sesionId = MutableStateFlow<Int?>(null)
        override suspend fun porUsuario(usuario: String) = usuarios.firstOrNull { it.usuario == usuario }
        override suspend fun insertar(usuario: UsuarioEntity): Long {
            val nuevo = usuario.copy(id = usuarios.size + 1)
            usuarios += nuevo
            return nuevo.id.toLong()
        }
        override suspend fun cambiarColor(id: Int, color: String) {
            usuarios.replaceAll { if (it.id == id) it.copy(colorIcono = color) else it }
            sesionId.value = sesionId.value // re-emitir
        }
        override suspend fun count() = usuarios.size
        override fun observeSesion(): Flow<UsuarioEntity?> = sesionId.map { id -> usuarios.firstOrNull { it.id == id } }
        override suspend fun abrirSesion(sesion: SesionEntity) { sesionId.value = sesion.usuarioId }
        override suspend fun cerrarSesion() { sesionId.value = null }
        override suspend fun sesionActual() = usuarios.firstOrNull { it.id == sesionId.value }
        override suspend fun actualizar(usuario: UsuarioEntity) { usuarios.replaceAll { if (it.id == usuario.id) usuario else it } }
        override suspend fun marcarSyncInicial(id: Int) { usuarios.replaceAll { if (it.id == id) it.copy(syncInicial = true) else it } }
    }

    private fun repo(dao: FakeUsuarioDao = FakeUsuarioDao(), scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) =
        AuthRepositoryImpl(dao, PasswordHasher(iteraciones = 1_000), LimitadorIntentos(), StandardTestDispatcher(scheduler))

    @Test fun `registrar guarda hash, normaliza el usuario y abre sesion`() = runTest {
        val dao = FakeUsuarioDao()
        val r = repo(dao, testScheduler)
        assertEquals(ResultadoCuenta.Ok, r.registrar("  Marce ", "mercado123", 0xFF5079B2.toInt()))
        val guardado = dao.usuarios.single()
        assertEquals("marce", guardado.usuario)
        assertFalse(guardado.passwordHash.contains("mercado123"))
        assertEquals("#5079B2", guardado.colorIcono)
        assertEquals("marce", r.sesion.first()?.usuario)
    }

    @Test fun `no permite usuario repetido`() = runTest {
        val r = repo(scheduler = testScheduler)
        r.registrar("marce", "mercado123", 0)
        assertEquals(ResultadoCuenta.Error(ErrorCuenta.USUARIO_OCUPADO), r.registrar("MARCE", "otra12345", 0))
    }

    @Test fun `login correcto, incorrecto y mismo mensaje si el usuario no existe`() = runTest {
        val r = repo(scheduler = testScheduler)
        r.registrar("marce", "mercado123", 0)
        r.cerrarSesion()
        assertNull(r.sesion.first())

        val credenciales = ResultadoCuenta.Error(ErrorCuenta.CREDENCIALES_INCORRECTAS)
        assertEquals(credenciales, r.iniciarSesion("marce", "equivocada"))
        assertEquals(credenciales, r.iniciarSesion("nadie", "equivocada"))
        assertEquals(ResultadoCuenta.Ok, r.iniciarSesion("Marce", "mercado123"))
        assertEquals("marce", r.sesion.first()?.usuario)
    }

    @Test fun `tras 5 fallos el login queda bloqueado`() = runTest {
        val r = repo(scheduler = testScheduler)
        r.registrar("marce", "mercado123", 0)
        repeat(5) { r.iniciarSesion("marce", "mal$it") }
        assert(r.iniciarSesion("marce", "mercado123") is ResultadoCuenta.Bloqueado)
    }
}
