package com.juntos.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Pasada la medianoche refresca el widget y, si está activo, el fondo de bloqueo. */
class ActualizacionDiaria(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        JuntosWidget().updateAll(applicationContext)
        if (FondoBloqueo.activo(applicationContext) && FondoBloqueo.tieneFoto(applicationContext)) {
            FondoBloqueo.aplicar(applicationContext)
        }
        return Result.success()
    }

    companion object {
        private const val NOMBRE = "actualizacion_diaria"

        fun programar(context: Context) {
            val ahora = LocalDateTime.now()
            val proxima = ahora.toLocalDate().plusDays(1).atTime(0, 1)
            val trabajo = PeriodicWorkRequestBuilder<ActualizacionDiaria>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(ahora, proxima))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NOMBRE, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, trabajo)
        }
    }
}
