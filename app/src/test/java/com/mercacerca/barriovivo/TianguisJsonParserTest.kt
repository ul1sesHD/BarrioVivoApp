package com.mercacerca.barriovivo

import com.mercacerca.barriovivo.data.seed.TianguisJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class TianguisJsonParserTest {

    private val muestra = """
        {"v":1,"campos":["id","dia","nombre","ubicacion","ini","fin","lat","lon","nota"],"total":3,"datos":[
          [1,"LUNES",null,"18 DE MARZO Y BENITO JUAREZ","08:00","19:00",19.347772,-98.991258,null],
          [409,"MIÉRCOLES",null,"BOULEVARD CARMEN SERDÁN","08:00","19:00",null,-99.102604,"coordenadas inválidas"],
          [7,"FERIADO",null,"SIN DIA","08:00","19:00",19.1,-99.1,null],
          [8,"SÁBADO","Tianguis del Sol","  ","08:00","19:00",19.1,-99.1,null]
        ]}
    """.trimIndent()

    @Test fun `descarta renglones sin dia valido o sin ubicacion`() {
        val r = TianguisJsonParser.parse(muestra)
        assertEquals(listOf(1, 409), r.map { it.id })
    }

    @Test fun `normaliza el dia y conserva campos`() {
        val r = TianguisJsonParser.parse(muestra).first { it.id == 409 }
        assertEquals("MIERCOLES", r.dia)
        assertEquals("08:00", r.horaIni)
        assertEquals("coordenadas inválidas", r.nota)
    }

    @Test fun `coordenada incompleta se guarda como null sin crashear`() {
        val r = TianguisJsonParser.parse(muestra).first { it.id == 409 }
        assertNull(r.lat)
        assertNull(r.lon)
    }

    @Test fun `parsea el dataset real completo`() {
        val archivo = File("src/main/assets/tianguis_cdmx.json")
        val r = TianguisJsonParser.parse(archivo.readText())
        assertEquals(1325, r.size)
        assertEquals(1, r.count { it.lat == null })
    }
}
