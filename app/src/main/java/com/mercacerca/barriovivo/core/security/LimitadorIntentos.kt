package com.mercacerca.barriovivo.core.security

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

/**
 * Frena la fuerza bruta en el login: tras [MAX_FALLOS] intentos fallidos seguidos,
 * bloquea [BLOQUEO_MS] milisegundos. Vive en memoria (es una app local, sin servidor).
 */
@Singleton
class LimitadorIntentos @Inject constructor() {
    internal var reloj: () -> Long = System::currentTimeMillis
    private var fallos = 0
    private var bloqueadoHasta = 0L

    @Synchronized
    fun segundosRestantes(): Int = ceil((bloqueadoHasta - reloj()) / 1000.0).toInt().coerceAtLeast(0)

    @Synchronized
    fun registrarFallo() {
        fallos++
        if (fallos >= MAX_FALLOS) {
            bloqueadoHasta = reloj() + BLOQUEO_MS
            fallos = 0
        }
    }

    @Synchronized
    fun registrarExito() {
        fallos = 0
    }

    companion object {
        const val MAX_FALLOS = 5
        const val BLOQUEO_MS = 30_000L
    }
}
