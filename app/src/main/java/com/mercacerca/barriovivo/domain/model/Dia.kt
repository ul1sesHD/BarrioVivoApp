package com.mercacerca.barriovivo.domain.model

import java.text.Normalizer
import java.util.Calendar

/** Día en que se pone un tianguis. El orden importa: lunes primero. */
enum class Dia(val corto: String, val largo: String) {
    LUNES("Lun", "lunes"),
    MARTES("Mar", "martes"),
    MIERCOLES("Mié", "miércoles"),
    JUEVES("Jue", "jueves"),
    VIERNES("Vie", "viernes"),
    SABADO("Sáb", "sábado"),
    DOMINGO("Dom", "domingo");

    companion object {
        /** "MIÉRCOLES", "sabado ", "Sábado" → [MIERCOLES], [SABADO]. Null si no es un día. */
        fun from(raw: String?): Dia? {
            if (raw == null) return null
            val limpio = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .uppercase()
            return entries.firstOrNull { it.name == limpio }
        }

        /** Calendar usa DOMINGO=1 … SÁBADO=7; lo pasamos a nuestro orden (LUNES=0). */
        fun hoy(calendar: Calendar = Calendar.getInstance()): Dia =
            entries[(calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7]
    }
}
