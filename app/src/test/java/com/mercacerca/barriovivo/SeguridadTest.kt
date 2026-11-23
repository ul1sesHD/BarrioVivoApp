package com.mercacerca.barriovivo

import com.mercacerca.barriovivo.core.security.LimitadorIntentos
import com.mercacerca.barriovivo.core.security.PasswordHasher
import com.mercacerca.barriovivo.domain.model.ErrorCuenta
import com.mercacerca.barriovivo.domain.model.ValidadorCuenta
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeguridadTest {

    private val hasher = PasswordHasher(iteraciones = 1_000) // pocas iteraciones: el test corre rápido

    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }

    @Test fun `pbkdf2 manual cumple los vectores de prueba publicados`() {
        // Vectores PBKDF2-HMAC-SHA256 (P="password", S="salt", dkLen=32).
        val salt = "salt".toByteArray()
        assertEquals("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b", hex(PasswordHasher.pbkdf2Manual("password", salt, 1)))
        assertEquals("ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43", hex(PasswordHasher.pbkdf2Manual("password", salt, 2)))
    }

    @Test fun `implementacion manual y la del sistema dan lo mismo`() {
        val salt = ByteArray(16) { it.toByte() }
        assertArrayEquals(
            PasswordHasher.pbkdf2Jca("mercado123ñ", salt, 500),
            PasswordHasher.pbkdf2Manual("mercado123ñ", salt, 500),
        )
    }

    @Test fun `verifica la correcta y rechaza la incorrecta`() {
        val h = hasher.hashear("mercado123")
        assertTrue(hasher.verificar("mercado123", h))
        assertFalse(hasher.verificar("mercado124", h))
        assertFalse(hasher.verificar("", h))
    }

    @Test fun `misma contrasena con distinto salt da distinto hash y nunca guarda el texto`() {
        val a = hasher.hashear("mercado123")
        val b = hasher.hashear("mercado123")
        assertNotEquals(a.saltHex, b.saltHex)
        assertNotEquals(a.hashHex, b.hashHex)
        assertFalse(a.hashHex.contains("mercado"))
        assertEquals(64, a.hashHex.length) // 256 bits
        assertEquals(32, a.saltHex.length) // 128 bits
    }

    @Test fun `validador de registro`() {
        assertNull(ValidadorCuenta.validarRegistro("marce_23", "mercado123"))
        assertEquals(ErrorCuenta.USUARIO_CORTO, ValidadorCuenta.validarRegistro("ab", "mercado123"))
        assertEquals(ErrorCuenta.USUARIO_LARGO, ValidadorCuenta.validarRegistro("a".repeat(21), "mercado123"))
        assertEquals(ErrorCuenta.USUARIO_CARACTERES, ValidadorCuenta.validarRegistro("peña", "mercado123"))
        assertEquals(ErrorCuenta.USUARIO_CARACTERES, ValidadorCuenta.validarRegistro("x' OR 1=1", "mercado123"))
        assertEquals(ErrorCuenta.PASSWORD_CORTA, ValidadorCuenta.validarRegistro("marce", "1234567"))
        assertEquals(ErrorCuenta.PASSWORD_LARGA, ValidadorCuenta.validarRegistro("marce", "a".repeat(65)))
        assertEquals(ErrorCuenta.PASSWORD_IGUAL_USUARIO, ValidadorCuenta.validarRegistro("marcela_23", "MARCELA_23"))
        assertEquals("marce", ValidadorCuenta.normalizarUsuario("  Marce "))
    }

    @Test fun `color ida y vuelta`() {
        assertEquals("#D90368", ValidadorCuenta.colorAHex(ValidadorCuenta.COLOR_DEFAULT))
        assertEquals(ValidadorCuenta.COLOR_DEFAULT, ValidadorCuenta.hexAColor("#D90368"))
        assertEquals(ValidadorCuenta.COLOR_DEFAULT, ValidadorCuenta.hexAColor("basura"))
    }

    @Test fun `limitador bloquea tras 5 fallos y libera despues de 30 s`() {
        var ahora = 0L
        val l = LimitadorIntentos().apply { reloj = { ahora } }
        repeat(4) { l.registrarFallo() }
        assertEquals(0, l.segundosRestantes())
        l.registrarFallo()
        assertEquals(30, l.segundosRestantes())
        ahora += 29_500
        assertEquals(1, l.segundosRestantes())
        ahora += 600
        assertEquals(0, l.segundosRestantes())
    }
}
