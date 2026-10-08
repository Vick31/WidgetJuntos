package com.juntos.widget

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Fondo de la pantalla de bloqueo: la foto escogida con una tarjeta de vidrio
 * difuminado encima que muestra el tiempo juntos.
 */
object FondoBloqueo {
    private const val ARCHIVO = "fondo"
    private const val POSICION = "posicion"
    private const val TAMANO = "tamano"
    private const val OSCURIDAD = "oscuridad"
    private const val DIFUMINADO = "difuminado"
    private const val ACTIVO = "activo"

    /** Lo que se personaliza de la tarjeta; todos los valores van de 0 a 1 salvo [tamano]. */
    data class Estilo(
        /** Centro vertical de la tarjeta, de 0 (arriba) a 1 (abajo). */
        val posicion: Float = 0.76f,
        /** 1 = ancho casi completo de la pantalla. */
        val tamano: Float = 0.85f,
        /** Cuánto oscurece el vidrio; 0 = totalmente transparente. */
        val oscuridad: Float = 0.12f,
        /** 0 = sin difuminar. */
        val difuminado: Float = 0.8f,
    )

    // Zonas que ocupa HyperOS en la pantalla de bloqueo (medidas en un Redmi Note 13, fracción del alto)
    private val ZONA_RELOJ = 0.08f..0.28f
    private val ZONA_NOTIFICACIONES = 0.28f..0.50f
    private const val INICIO_ACCESOS = 0.86f

    private fun prefs(context: Context) = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    private fun archivoFoto(context: Context) = File(context.filesDir, "foto_bloqueo")

    fun tieneFoto(context: Context) = archivoFoto(context).exists()

    /** Copia la foto a la app para no depender de que siga en la galería. */
    fun guardarFoto(context: Context, uri: Uri) {
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            archivoFoto(context).outputStream().use { entrada.copyTo(it) }
        }
    }

    fun estilo(context: Context): Estilo {
        val p = prefs(context)
        val inicial = Estilo()
        return Estilo(
            posicion = p.getFloat(POSICION, inicial.posicion),
            tamano = p.getFloat(TAMANO, inicial.tamano),
            oscuridad = p.getFloat(OSCURIDAD, inicial.oscuridad),
            difuminado = p.getFloat(DIFUMINADO, inicial.difuminado),
        )
    }

    fun guardarEstilo(context: Context, estilo: Estilo) {
        prefs(context).edit()
            .putFloat(POSICION, estilo.posicion)
            .putFloat(TAMANO, estilo.tamano)
            .putFloat(OSCURIDAD, estilo.oscuridad)
            .putFloat(DIFUMINADO, estilo.difuminado)
            .apply()
    }

    /** Si está activo, la actualización diaria vuelve a generar el fondo. */
    fun activo(context: Context) = prefs(context).getBoolean(ACTIVO, false)

    fun guardarActivo(context: Context, activo: Boolean) {
        prefs(context).edit().putBoolean(ACTIVO, activo).apply()
    }

    /** Tamaño real de la pantalla en vertical (ancho, alto). */
    fun tamanoPantalla(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(WindowManager::class.java)
        val (a, b) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.maximumWindowMetrics.bounds.let { it.width() to it.height() }
        } else {
            val metricas = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metricas)
            metricas.widthPixels to metricas.heightPixels
        }
        return minOf(a, b) to maxOf(a, b)
    }

    /** Genera el fondo con los datos guardados y lo pone en la pantalla de bloqueo. */
    fun aplicar(context: Context): Boolean = runCatching {
        val (ancho, alto) = tamanoPantalla(context)
        val imagen = generar(context, Pareja.leer(context), ancho, alto, estilo(context)) ?: return false
        WallpaperManager.getInstance(context).setBitmap(imagen, null, true, WallpaperManager.FLAG_LOCK)
        true
    }.getOrDefault(false)

    /** [guias] marca dónde quedan el reloj, las notificaciones y la huella (solo para la vista previa). */
    fun generar(
        context: Context,
        pareja: Pareja,
        ancho: Int,
        alto: Int,
        estilo: Estilo,
        guias: Boolean = false,
    ): Bitmap? {
        val foto = decodificar(archivoFoto(context), ancho, alto) ?: return null
        val salida = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(salida)

        // Recorte centrado para llenar la pantalla sin deformar la foto
        val escala = max(ancho / foto.width.toFloat(), alto / foto.height.toFloat())
        val w = foto.width * escala
        val h = foto.height * escala
        val destino = RectF((ancho - w) / 2, (alto - h) / 2, (ancho + w) / 2, (alto + h) / 2)
        canvas.drawBitmap(foto, null, destino, Paint(Paint.FILTER_BITMAP_FLAG))

        val inicio = pareja.inicio
        if (inicio != null) {
            dibujarTarjeta(canvas, salida, pareja, inicio.enTexto(), TiempoJuntos.calcular(inicio), estilo)
        }
        if (guias) dibujarGuias(canvas)
        return salida
    }

    private fun dibujarGuias(canvas: Canvas) {
        val u = canvas.width / 100f
        val alto = canvas.height
        val relleno = Paint().apply { color = 0x26FFFFFF }
        val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f * u
            color = 0xB3FFFFFF.toInt()
            pathEffect = DashPathEffect(floatArrayOf(2 * u, 1.5f * u), 0f)
        }
        val texto = pincel(3.4f * u, negrita = true)
        fun zona(desde: Float, hasta: Float, nombre: String) {
            val r = RectF(2 * u, alto * desde, canvas.width - 2 * u, alto * hasta)
            canvas.drawRoundRect(r, 3 * u, 3 * u, relleno)
            canvas.drawRoundRect(r, 3 * u, 3 * u, borde)
            texto.escribir(canvas, nombre, r.centerX(), r.top + 1.5f * u)
        }
        zona(ZONA_RELOJ.start, ZONA_RELOJ.endInclusive, "Reloj")
        zona(ZONA_NOTIFICACIONES.start, ZONA_NOTIFICACIONES.endInclusive, "Notificaciones")
        zona(INICIO_ACCESOS, 0.995f, "Huella y accesos")
    }

    private fun decodificar(archivo: File, ancho: Int, alto: Int): Bitmap? = runCatching {
        if (!archivo.exists()) return null
        // ImageDecoder respeta la orientación EXIF de las fotos de la cámara
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(archivo)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val s = max(ancho / info.size.width.toFloat(), alto / info.size.height.toFloat())
            if (s < 1f) {
                decoder.setTargetSize(
                    (info.size.width * s).roundToInt().coerceAtLeast(1),
                    (info.size.height * s).roundToInt().coerceAtLeast(1),
                )
            }
        }
    }.getOrNull()

    private fun dibujarTarjeta(
        canvas: Canvas,
        imagen: Bitmap,
        pareja: Pareja,
        desde: String,
        tiempo: TiempoJuntos,
        estilo: Estilo,
    ) {
        // Todo se mide en "u" = 1 % del ancho, para que se vea igual en cualquier pantalla;
        // "t" es la misma unidad escalada por el tamaño escogido
        val u = canvas.width / 100f
        val t = u * estilo.tamano
        val margen = 4 * u
        val relleno = 3.5f * t
        val radio = 5 * t

        val titulo = pincel(4f * t, negrita = true)
        val numero = pincel(8.5f * t, negrita = true)
        val etiqueta = pincel(3.2f * t, alfa = 0.9f)
        val total = pincel(3.7f * t, negrita = true)
        val fecha = pincel(2.9f * t, alfa = 0.85f)

        val anchoTarjeta = 86 * t
        val altoTarjeta = relleno * 2 + titulo.alto() + 1 * t + numero.alto() + etiqueta.alto() +
            1.5f * t + total.alto() + fecha.alto()
        // Nunca por encima del borde ni encima de la huella y los accesos de abajo
        val arriba = (canvas.height * estilo.posicion - altoTarjeta / 2)
            .coerceIn(margen, canvas.height * INICIO_ACCESOS - altoTarjeta - 1.5f * u)
        val izquierda = (canvas.width - anchoTarjeta) / 2
        val tarjeta = RectF(izquierda, arriba, izquierda + anchoTarjeta, arriba + altoTarjeta)

        // Vidrio: la parte de la foto detrás de la tarjeta, difuminada y oscurecida
        val forma = Path().apply { addRoundRect(tarjeta, radio, radio, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(forma)
        val radioDifuminado = (estilo.difuminado * 6).roundToInt()
        if (radioDifuminado > 0) {
            val zona = Rect(tarjeta.left.toInt(), tarjeta.top.toInt(), tarjeta.right.toInt(), tarjeta.bottom.toInt())
            val recorte = Bitmap.createBitmap(imagen, zona.left, zona.top, zona.width(), zona.height())
            val difuminado = Bitmap.createScaledBitmap(
                recorte, max(1, zona.width() / 8), max(1, zona.height() / 8), true,
            ).copy(Bitmap.Config.ARGB_8888, true)
            desenfocar(difuminado, radio = radioDifuminado, pasadas = 3)
            canvas.drawBitmap(difuminado, null, tarjeta, Paint(Paint.FILTER_BITMAP_FLAG))
        }
        canvas.drawColor(android.graphics.Color.argb((estilo.oscuridad * 255).roundToInt(), 0, 0, 0))
        canvas.restore()

        val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, 0.3f * u)
            color = 0x4DFFFFFF
        }
        canvas.drawRoundRect(tarjeta, radio, radio, borde)

        // Texto
        val centro = tarjeta.centerX()
        var y = tarjeta.top + relleno
        y = titulo.escribir(canvas, pareja.titulo, centro, y) + 1 * t

        val columnas = listOf(
            tiempo.anios to tiempo.textoAnios,
            tiempo.meses to tiempo.textoMeses,
            tiempo.dias to tiempo.textoDias,
        )
        columnas.forEachIndexed { i, (valor, texto) ->
            val x = tarjeta.left + tarjeta.width() * (2 * i + 1) / 6
            val yEtiqueta = numero.escribir(canvas, valor.toString(), x, y)
            etiqueta.escribir(canvas, texto, x, yEtiqueta)
        }
        y += numero.alto() + etiqueta.alto() + 1.5f * t

        val lineaTotal = if (tiempo.esAniversario) "¡Feliz aniversario! 🎉" else "${tiempo.totalDias.conMiles()} días juntos"
        y = total.escribir(canvas, lineaTotal, centro, y)
        fecha.escribir(canvas, "Desde el $desde", centro, y)
    }

    private fun pincel(tamano: Float, negrita: Boolean = false, alfa: Float = 1f) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = tamano
            textAlign = Paint.Align.CENTER
            typeface = if (negrita) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            color = android.graphics.Color.argb((alfa * 255).roundToInt(), 255, 255, 255)
            // Sombra marcada: con el vidrio casi transparente es lo que mantiene el texto legible
            setShadowLayer(tamano * 0.1f, 0f, tamano * 0.03f, 0x8C000000.toInt())
        }

    private fun Paint.alto() = descent() - ascent()

    /** Escribe una línea centrada en x con su borde superior en y; devuelve dónde empieza la siguiente. */
    private fun Paint.escribir(canvas: Canvas, texto: String, x: Float, y: Float): Float {
        canvas.drawText(texto, x, y - ascent(), this)
        return y + alto()
    }

    /** Desenfoque de caja en varias pasadas (se aproxima a un gaussiano). */
    private fun desenfocar(imagen: Bitmap, radio: Int, pasadas: Int) {
        val w = imagen.width
        val h = imagen.height
        val pixeles = IntArray(w * h)
        val temporal = IntArray(w * h)
        imagen.getPixels(pixeles, 0, w, 0, 0, w, h)
        repeat(pasadas) {
            caja(pixeles, temporal, w, h, radio, horizontal = true)
            caja(temporal, pixeles, w, h, radio, horizontal = false)
        }
        imagen.setPixels(pixeles, 0, w, 0, 0, w, h)
    }

    private fun caja(origen: IntArray, destino: IntArray, w: Int, h: Int, radio: Int, horizontal: Boolean) {
        val n = 2 * radio + 1
        for (y in 0 until h) {
            for (x in 0 until w) {
                var a = 0
                var r = 0
                var g = 0
                var b = 0
                for (k in -radio..radio) {
                    val c = if (horizontal) {
                        origen[y * w + (x + k).coerceIn(0, w - 1)]
                    } else {
                        origen[(y + k).coerceIn(0, h - 1) * w + x]
                    }
                    a += c ushr 24
                    r += (c shr 16) and 0xFF
                    g += (c shr 8) and 0xFF
                    b += c and 0xFF
                }
                destino[y * w + x] = ((a / n) shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
            }
        }
    }
}
