package com.juntos.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class JuntosWidget : GlanceAppWidget() {

    companion object {
        private val PEQUENO = DpSize(110.dp, 50.dp)
        private val ANCHO = DpSize(220.dp, 50.dp)
        private val GRANDE = DpSize(220.dp, 150.dp)
    }

    // El launcher escoge el diseño según el tamaño al que se estire el widget.
    override val sizeMode = SizeMode.Responsive(setOf(PEQUENO, ANCHO, GRANDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val pareja = Pareja.leer(context)
        provideContent {
            Contenido(pareja)
        }
    }

    @Composable
    private fun Contenido(pareja: Pareja) {
        val tamano = LocalSize.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.fondo_widget))
                .cornerRadius(24.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val inicio = pareja.inicio
            if (inicio == null) {
                TextoBlanco(pareja.titulo, 16, negrita = true)
                TextoBlanco("Toca para escoger la fecha", 12)
                return@Column
            }

            val tiempo = TiempoJuntos.calcular(inicio)
            TextoBlanco(pareja.titulo, if (tamano.width >= ANCHO.width) 15 else 13, negrita = true)
            Spacer(GlanceModifier.height(4.dp))

            if (tamano.width >= ANCHO.width) {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Bloque(tiempo.anios.toString(), tiempo.textoAnios, GlanceModifier.defaultWeight())
                    Bloque(tiempo.meses.toString(), tiempo.textoMeses, GlanceModifier.defaultWeight())
                    Bloque(tiempo.dias.toString(), tiempo.textoDias, GlanceModifier.defaultWeight())
                }
                if (tamano.height >= GRANDE.height) {
                    Spacer(GlanceModifier.height(6.dp))
                    TextoBlanco("${tiempo.totalDias.conMiles()} días juntos", 14, negrita = true)
                }
            } else {
                TextoBlanco(tiempo.totalDias.conMiles(), 30, negrita = true)
                TextoBlanco("días juntos", 12)
            }

            Spacer(GlanceModifier.height(4.dp))
            if (tiempo.esAniversario) {
                TextoBlanco("¡Feliz aniversario! 🎉", 12, negrita = true)
            } else if (tamano.height >= GRANDE.height || tamano.width < ANCHO.width) {
                TextoBlanco("Desde el ${inicio.enTexto()}", 11, alfa = 0.85f)
            }
        }
    }

    @Composable
    private fun Bloque(numero: String, etiqueta: String, modifier: GlanceModifier) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            TextoBlanco(numero, 28, negrita = true)
            TextoBlanco(etiqueta, 12, alfa = 0.9f)
        }
    }

    @Composable
    private fun TextoBlanco(texto: String, tamano: Int, negrita: Boolean = false, alfa: Float = 1f) {
        Text(
            text = texto,
            style = TextStyle(
                color = ColorProvider(Color.White.copy(alpha = alfa)),
                fontSize = tamano.sp,
                fontWeight = if (negrita) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

class JuntosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = JuntosWidget()
}
