package com.juntos.widget

import android.content.Context
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Lo que se configura en la app y muestra el widget. */
data class Pareja(
    val nombre1: String,
    val nombre2: String,
    val inicio: LocalDate?,
) {
    val titulo: String
        get() = when {
            nombre1.isNotBlank() && nombre2.isNotBlank() -> "$nombre1 ♥ $nombre2"
            else -> "♥ Juntos ♥"
        }

    companion object {
        private const val ARCHIVO = "juntos"
        private const val NOMBRE1 = "nombre1"
        private const val NOMBRE2 = "nombre2"
        private const val INICIO = "inicio"

        fun leer(context: Context): Pareja {
            val prefs = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            val inicio = if (prefs.contains(INICIO)) LocalDate.ofEpochDay(prefs.getLong(INICIO, 0)) else null
            return Pareja(
                nombre1 = prefs.getString(NOMBRE1, "") ?: "",
                nombre2 = prefs.getString(NOMBRE2, "") ?: "",
                inicio = inicio,
            )
        }

        fun guardar(context: Context, pareja: Pareja) {
            context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().apply {
                putString(NOMBRE1, pareja.nombre1.trim())
                putString(NOMBRE2, pareja.nombre2.trim())
                if (pareja.inicio != null) putLong(INICIO, pareja.inicio.toEpochDay()) else remove(INICIO)
            }.apply()
        }
    }
}

/** Tiempo transcurrido entre la fecha de inicio y hoy. */
data class TiempoJuntos(
    val anios: Int,
    val meses: Int,
    val dias: Int,
    val totalDias: Long,
    val esAniversario: Boolean,
) {
    val textoAnios get() = if (anios == 1) "año" else "años"
    val textoMeses get() = if (meses == 1) "mes" else "meses"
    val textoDias get() = if (dias == 1) "día" else "días"

    companion object {
        fun calcular(inicio: LocalDate, hoy: LocalDate = LocalDate.now()): TiempoJuntos {
            val periodo = Period.between(inicio, hoy)
            return TiempoJuntos(
                anios = periodo.years,
                meses = periodo.months,
                dias = periodo.days,
                totalDias = ChronoUnit.DAYS.between(inicio, hoy),
                esAniversario = periodo.years > 0 &&
                    hoy.monthValue == inicio.monthValue &&
                    hoy.dayOfMonth == inicio.dayOfMonth,
            )
        }
    }
}

private val formatoFecha = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es"))

fun LocalDate.enTexto(): String = format(formatoFecha)

/** 12345 -> "12.345" */
fun Long.conMiles(): String = "%,d".format(Locale.forLanguageTag("es-CO"), this)
