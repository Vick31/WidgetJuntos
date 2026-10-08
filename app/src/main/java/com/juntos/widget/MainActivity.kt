package com.juntos.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val Rosa = Color(0xFFD81B60)
private val Naranja = Color(0xFFFF7043)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Rosa, secondary = Naranja)) {
                Pantalla(Pareja.leer(this))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Pantalla(guardada: Pareja) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nombre1 by remember { mutableStateOf(guardada.nombre1) }
    var nombre2 by remember { mutableStateOf(guardada.nombre2) }
    var inicio by remember { mutableStateOf(guardada.inicio) }
    var mostrarCalendario by remember { mutableStateOf(false) }

    val pareja = Pareja(nombre1, nombre2, inicio)

    Scaffold { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Nuestro tiempo juntos", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            VistaPrevia(pareja)

            OutlinedTextField(
                value = nombre1,
                onValueChange = { nombre1 = it },
                label = { Text("Tu nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = nombre2,
                onValueChange = { nombre2 = it },
                label = { Text("Nombre de tu esposa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = { mostrarCalendario = true }, modifier = Modifier.fillMaxWidth()) {
                Text(inicio?.let { "Juntos desde el ${it.enTexto()}" } ?: "Escoger la fecha en que empezaron")
            }

            Button(
                onClick = {
                    Pareja.guardar(context, pareja)
                    scope.launch { JuntosWidget().updateAll(context) }
                    Toast.makeText(context, "Guardado ♥", Toast.LENGTH_SHORT).show()
                },
                enabled = inicio != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Guardar")
            }

            OutlinedButton(onClick = { fijarWidget(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("Poner el widget en la pantalla de inicio")
            }

            Text(
                "También puedes mantener presionado el fondo de pantalla → Widgets → Juntos. " +
                    "Si lo estiras a lo ancho muestra años, meses y días.",
                fontSize = 13.sp,
                color = Color.Gray,
            )
        }
    }

    if (mostrarCalendario) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = inicio?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = aFecha(utcTimeMillis) <= LocalDate.now()
            },
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { inicio = aFecha(it) }
                    mostrarCalendario = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = estado)
        }
    }
}

/** Réplica del widget, sobre un degradado que hace de fondo de pantalla. */
@Composable
private fun VistaPrevia(pareja: Pareja) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Rosa, Naranja)))
            .padding(16.dp),
    ) {
        TarjetaVidrio(pareja)
    }
}

@Composable
private fun TarjetaVidrio(pareja: Pareja) {
    val inicio = pareja.inicio
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(pareja.titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        if (inicio == null) {
            Text("Escoge la fecha para empezar a contar", color = Color.White)
            return@Column
        }
        val tiempo = TiempoJuntos.calcular(inicio)
        Row(modifier = Modifier.fillMaxWidth()) {
            Bloque(tiempo.anios, tiempo.textoAnios, Modifier.weight(1f))
            Bloque(tiempo.meses, tiempo.textoMeses, Modifier.weight(1f))
            Bloque(tiempo.dias, tiempo.textoDias, Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Text("${tiempo.totalDias.conMiles()} días juntos", color = Color.White, fontWeight = FontWeight.Bold)
        Text(
            if (tiempo.esAniversario) "¡Feliz aniversario! 🎉" else "Desde el ${inicio.enTexto()}",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun Bloque(numero: Int, etiqueta: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(numero.toString(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(etiqueta, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

// El DatePicker de Material trabaja en milisegundos UTC a medianoche.
private fun aFecha(utcMillis: Long): LocalDate =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

private fun fijarWidget(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    if (manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(ComponentName(context, JuntosWidgetReceiver::class.java), null, null)
    } else {
        Toast.makeText(context, "Agrégalo desde el menú de widgets del fondo de pantalla", Toast.LENGTH_LONG).show()
    }
}
