package com.mercacerca.barriovivo.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.mercacerca.barriovivo.core.security.LimitadorIntentos
import com.mercacerca.barriovivo.core.security.PasswordHash
import com.mercacerca.barriovivo.core.security.PasswordHasher
import com.mercacerca.barriovivo.data.local.SesionEntity
import com.mercacerca.barriovivo.data.local.UsuarioDao
import com.mercacerca.barriovivo.data.local.UsuarioEntity
import com.mercacerca.barriovivo.data.remote.NubeApagada
import com.mercacerca.barriovivo.data.remote.NubeRemota
import com.mercacerca.barriovivo.data.remote.ResultadoNube
import com.mercacerca.barriovivo.data.sync.SolicitudSync
import com.mercacerca.barriovivo.di.DefaultDispatcher
import com.mercacerca.barriovivo.domain.model.ErrorCuenta
import com.mercacerca.barriovivo.domain.model.ResultadoCuenta
import com.mercacerca.barriovivo.domain.model.Usuario
import com.mercacerca.barriovivo.domain.model.ValidadorCuenta
import com.mercacerca.barriovivo.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Cuentas locales (PBKDF2 en Room) + Supabase Auth cuando hay nube.
 * - Con nube: el registro y el login van a Supabase; la copia local permite entrar sin internet.
 * - Sin nube (o sin internet al entrar): se verifica contra el hash local, como en la fase 4.
 */
class AuthRepositoryImpl @Inject constructor(
    private val dao: UsuarioDao,
    private val hasher: PasswordHasher,
    private val limitador: LimitadorIntentos,
    @param:DefaultDispatcher private val cpu: CoroutineDispatcher, // el hash es trabajo pesado de CPU
    private val nube: NubeRemota = NubeApagada,
    private val sync: SolicitudSync = SolicitudSync {},
) : AuthRepository {

    override val sesion: Flow<Usuario?> = dao.observeSesion().map { it?.toDomain() }

    override suspend fun registrar(usuario: String, password: String, colorArgb: Int): ResultadoCuenta {
        val nombre = ValidadorCuenta.normalizarUsuario(usuario)
        ValidadorCuenta.validarRegistro(nombre, password)?.let { return ResultadoCuenta.Error(it) }
        if (dao.porUsuario(nombre) != null) return ResultadoCuenta.Error(ErrorCuenta.USUARIO_OCUPADO)
        val colorHex = ValidadorCuenta.colorAHex(colorArgb)

        // Con nube, el nombre tiene que ser único en TODA la comunidad: se registra primero allá.
        val remoteId = if (nube.activa) {
            when (val r = nube.registrar(nombre, password, colorHex)) {
                is ResultadoNube.Ok -> r.valor
                ResultadoNube.UsuarioOcupado -> return ResultadoCuenta.Error(ErrorCuenta.USUARIO_OCUPADO)
                ResultadoNube.SinConexion -> return ResultadoCuenta.Error(ErrorCuenta.SIN_CONEXION)
                else -> return ResultadoCuenta.Error(ErrorCuenta.ERROR_NUBE)
            }
        } else null

        val hash = withContext(cpu) { hasher.hashear(password) }
        val id = try {
            dao.insertar(nuevoUsuario(nombre, hash, colorHex, remoteId))
        } catch (_: SQLiteConstraintException) { // otro registro le ganó el nombre
            return ResultadoCuenta.Error(ErrorCuenta.USUARIO_OCUPADO)
        }
        abrirSesion(id.toInt())
        return ResultadoCuenta.Ok
    }

    override suspend fun iniciarSesion(usuario: String, password: String): ResultadoCuenta {
        limitador.segundosRestantes().takeIf { it > 0 }?.let { return ResultadoCuenta.Bloqueado(it) }
        val nombre = ValidadorCuenta.normalizarUsuario(usuario)

        if (nube.activa) {
            when (val r = nube.iniciarSesion(nombre, password)) {
                is ResultadoNube.Ok -> {
                    // Copia local al día (hash nuevo = puedes entrar offline con esta contraseña).
                    val hash = withContext(cpu) { hasher.hashear(password) }
                    val local = dao.porUsuario(nombre)
                    val id = if (local == null) {
                        dao.insertar(nuevoUsuario(nombre, hash, r.valor.colorIcono, r.valor.id)).toInt()
                    } else {
                        dao.actualizar(
                            local.copy(
                                passwordHash = hash.hashHex, passwordSalt = hash.saltHex, passwordIter = hash.iteraciones,
                                colorIcono = r.valor.colorIcono, remoteId = r.valor.id,
                            )
                        )
                        local.id
                    }
                    return exito(id)
                }
                ResultadoNube.CredencialesInvalidas -> {
                    // ¿Cuenta creada antes de la nube? Si la contraseña local es correcta, entra y se sube.
                    val local = dao.porUsuario(nombre)
                    if (local?.remoteId == null && local != null && verificarLocal(local, password)) {
                        (nube.registrar(nombre, password, local.colorIcono) as? ResultadoNube.Ok)
                            ?.let { dao.actualizar(local.copy(remoteId = it.valor)) }
                        return exito(local.id)
                    }
                    limitador.registrarFallo()
                    return ResultadoCuenta.Error(ErrorCuenta.CREDENCIALES_INCORRECTAS)
                }
                else -> Unit // sin internet o error del servidor: se intenta con la copia local
            }
        }

        val guardado = dao.porUsuario(nombre)
        val valida = withContext(cpu) {
            if (guardado != null) {
                verificarLocal(guardado, password)
            } else {
                hasher.hashear(password) // mismo tiempo de respuesta exista o no el usuario
                false
            }
        }
        if (!valida || guardado == null) {
            limitador.registrarFallo()
            return ResultadoCuenta.Error(ErrorCuenta.CREDENCIALES_INCORRECTAS)
        }
        return exito(guardado.id)
    }

    override suspend fun cambiarColor(colorArgb: Int) {
        val actual = sesion.first() ?: return
        val hex = ValidadorCuenta.colorAHex(colorArgb)
        dao.cambiarColor(actual.id, hex)
        if (nube.activa) nube.cambiarColor(hex) // si falla no pasa nada: el color local manda
    }

    override suspend fun cerrarSesion() {
        dao.cerrarSesion()
        nube.cerrarSesion()
    }

    // ---- ayudantes ----

    private suspend fun exito(usuarioId: Int): ResultadoCuenta {
        limitador.registrarExito()
        abrirSesion(usuarioId)
        return ResultadoCuenta.Ok
    }

    private suspend fun abrirSesion(usuarioId: Int) {
        dao.abrirSesion(SesionEntity(usuarioId = usuarioId))
        sync.solicitar() // sube/baja tus datos en segundo plano
    }

    private suspend fun verificarLocal(u: UsuarioEntity, password: String) = withContext(cpu) {
        hasher.verificar(password, PasswordHash(u.passwordHash, u.passwordSalt, u.passwordIter))
    }

    private fun nuevoUsuario(nombre: String, hash: PasswordHash, colorHex: String, remoteId: String?) = UsuarioEntity(
        usuario = nombre,
        passwordHash = hash.hashHex,
        passwordSalt = hash.saltHex,
        passwordIter = hash.iteraciones,
        colorIcono = colorHex,
        remoteId = remoteId,
    )
}

private fun UsuarioEntity.toDomain() = Usuario(id, usuario, ValidadorCuenta.hexAColor(colorIcono), creadoEn)
