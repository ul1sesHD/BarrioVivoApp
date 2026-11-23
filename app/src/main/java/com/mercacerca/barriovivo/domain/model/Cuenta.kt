package com.mercacerca.barriovivo.domain.model

/** Perfil local. Sin correo, sin teléfono, sin foto: usuario + color de su ícono. */
data class Usuario(val id: Int, val usuario: String, val colorArgb: Int, val creadoEn: Long)

enum class ErrorCuenta {
    USUARIO_CORTO, USUARIO_LARGO, USUARIO_CARACTERES,
    PASSWORD_CORTA, PASSWORD_LARGA, PASSWORD_IGUAL_USUARIO,
    USUARIO_OCUPADO,
    /** Mensaje genérico a propósito: no dice si falló el usuario o la contraseña. */
    CREDENCIALES_INCORRECTAS,
    /** Con nube, registrarse necesita internet (el nombre debe ser único en toda la comunidad). */
    SIN_CONEXION,
    ERROR_NUBE,
}

sealed interface ResultadoCuenta {
    data object Ok : ResultadoCuenta
    data class Error(val motivo: ErrorCuenta) : ResultadoCuenta
    data class Bloqueado(val segundos: Int) : ResultadoCuenta
}

/** Reglas de usuario y contraseña (capa domain: no depende de Android). */
object ValidadorCuenta {
    const val USUARIO_MIN = 3
    const val USUARIO_MAX = 20
    const val PASSWORD_MIN = 8
    /** Tope para que nadie mande una contraseña gigante y trabe el hash. */
    const val PASSWORD_MAX = 64
    private val PERMITIDOS = Regex("^[a-z0-9_.]+$")

    /** "  Marce " → "marce". El usuario se guarda y se busca siempre así. */
    fun normalizarUsuario(usuario: String): String = usuario.trim().lowercase()

    /** Null si todo está bien. Espera el usuario ya normalizado. */
    fun validarRegistro(usuario: String, password: String): ErrorCuenta? = when {
        usuario.length < USUARIO_MIN -> ErrorCuenta.USUARIO_CORTO
        usuario.length > USUARIO_MAX -> ErrorCuenta.USUARIO_LARGO
        !PERMITIDOS.matches(usuario) -> ErrorCuenta.USUARIO_CARACTERES
        password.length < PASSWORD_MIN -> ErrorCuenta.PASSWORD_CORTA
        password.length > PASSWORD_MAX -> ErrorCuenta.PASSWORD_LARGA
        password.equals(usuario, ignoreCase = true) -> ErrorCuenta.PASSWORD_IGUAL_USUARIO
        else -> null
    }

    /** Color por defecto del ícono: rosa mexicano. */
    const val COLOR_DEFAULT: Int = 0xFFD90368.toInt()

    fun colorAHex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

    fun hexAColor(hex: String): Int =
        hex.removePrefix("#").takeIf { it.length == 6 }?.toLongOrNull(16)?.let { (0xFF000000 or it).toInt() }
            ?: COLOR_DEFAULT
}
