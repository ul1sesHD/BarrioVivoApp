package com.mercacerca.barriovivo.core.security

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Lo único que se guarda de una contraseña: nunca el texto plano. */
data class PasswordHash(val hashHex: String, val saltHex: String, val iteraciones: Int)

/**
 * PBKDF2-HMAC-SHA256 con salt aleatorio de 16 bytes por usuario y llave de 256 bits.
 *
 * Android trae "PBKDF2WithHmacSHA256" desde la API 26; en Android 7 (API 24-25) no existe,
 * así que hay una implementación propia de PBKDF2 (RFC 8018) sobre HmacSHA256, que sí existe.
 * Ambas dan exactamente el mismo resultado (hay un test que lo comprueba).
 */
class PasswordHasher(
    private val iteraciones: Int = ITERACIONES,
    private val random: SecureRandom = SecureRandom(),
) {
    fun hashear(password: String): PasswordHash {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return PasswordHash(derivar(password, salt, iteraciones).toHex(), salt.toHex(), iteraciones)
    }

    fun verificar(password: String, guardado: PasswordHash): Boolean {
        val calculado = derivar(password, guardado.saltHex.hexToBytes(), guardado.iteraciones)
        // Comparación en tiempo constante: no revela cuántos bytes coincidieron.
        return MessageDigest.isEqual(calculado, guardado.hashHex.hexToBytes())
    }

    companion object {
        /** Recomendación OWASP para PBKDF2-SHA256 (2021). Sube el número si el teléfono aguanta. */
        const val ITERACIONES = 310_000
        private const val SALT_BYTES = 16
        private const val LLAVE_BITS = 256

        fun derivar(password: String, salt: ByteArray, iteraciones: Int): ByteArray = try {
            pbkdf2Jca(password, salt, iteraciones)
        } catch (_: NoSuchAlgorithmException) {
            pbkdf2Manual(password, salt, iteraciones)
        }

        internal fun pbkdf2Jca(password: String, salt: ByteArray, iteraciones: Int): ByteArray {
            val spec = PBEKeySpec(password.toCharArray(), salt, iteraciones, LLAVE_BITS)
            return try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            } finally {
                spec.clearPassword() // borra la copia de la contraseña en memoria
            }
        }

        /** PBKDF2 a mano: con 256 bits de salida basta un bloque T1 = U1 xor U2 xor … xor Uc. */
        internal fun pbkdf2Manual(password: String, salt: ByteArray, iteraciones: Int): ByteArray {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(password.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            var u = mac.doFinal(salt + byteArrayOf(0, 0, 0, 1)) // salt || INT(1)
            val t = u.copyOf()
            repeat(iteraciones - 1) {
                u = mac.doFinal(u)
                for (i in t.indices) t[i] = (t[i].toInt() xor u[i].toInt()).toByte()
            }
            return t
        }

        private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
        private fun String.hexToBytes() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
