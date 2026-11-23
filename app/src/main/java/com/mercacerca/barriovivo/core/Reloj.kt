package com.mercacerca.barriovivo.core

import javax.inject.Inject

/** La hora actual, inyectable: en los tests se usa un reloj fijo. */
fun interface Reloj {
    fun ahora(): Long
}

class RelojSistema @Inject constructor() : Reloj {
    override fun ahora(): Long = System.currentTimeMillis()
}
