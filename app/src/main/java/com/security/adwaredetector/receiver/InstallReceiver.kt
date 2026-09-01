package com.security.adwaredetector.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.security.adwaredetector.notification.NotificationHelper
import com.security.adwaredetector.scanner.AdwareScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver que escucha ACTION_PACKAGE_ADDED / ACTION_PACKAGE_REPLACED.
 *
 * Flujo:
 *  1. El sistema instala una nueva app y envía el broadcast con el paquete afectado.
 *  2. Extraemos el nombre del paquete desde el Intent.data (formato "package:com.foo.bar").
 *  3. Analizamos el paquete en segundo plano con AdwareScanner.
 *  4. Si el score >= 60 (riesgo ALTO), disparamos una notificación de alta prioridad.
 *
 * Nota: goAsync() se usa para poder ejecutar trabajo asíncrono (I/O de PackageManager)
 * sin que el sistema mate el proceso antes de terminar, ya que onReceive() normal
 * debe retornar rápido.
 */
class InstallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "InstallReceiver"
        private const val HIGH_RISK_THRESHOLD = 60
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_PACKAGE_ADDED && action != Intent.ACTION_PACKAGE_REPLACED) {
            return
        }

        // Ignorar actualizaciones de la propia app sobre sí misma para evitar ruido
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        val packageName = intent.data?.encodedSchemeSpecificPart
        if (packageName.isNullOrBlank()) {
            Log.w(TAG, "Broadcast recibido sin nombre de paquete válido")
            return
        }
        if (packageName == context.packageName) {
            // No nos auto-analizamos
            return
        }

        Log.d(TAG, "Paquete detectado: $packageName (replacing=$isReplacing)")

        // goAsync() extiende el ciclo de vida del receiver para permitir trabajo async
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scanner = AdwareScanner(context.applicationContext)
                val result = scanner.analyzePackage(packageName)

                if (result != null && !result.isSystemApp && result.score >= HIGH_RISK_THRESHOLD) {
                    Log.w(TAG, "Riesgo ALTO detectado en $packageName (score=${result.score})")
                    // Las notificaciones deben construirse en un contexto válido;
                    // NotificationHelper ya usa NotificationManagerCompat internamente.
                    ContextCompat.getMainExecutor(context).execute {
                        NotificationHelper.showHighRiskAlert(context, result)
                    }
                } else {
                    Log.d(TAG, "Paquete $packageName analizado sin riesgo alto (score=${result?.score ?: -1})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error analizando el paquete recién instalado: $packageName", e)
            } finally {
                // Fundamental: siempre liberar el pendingResult para que el sistema
                // pueda finalizar el ciclo de vida del BroadcastReceiver.
                pendingResult.finish()
            }
        }
    }
}
