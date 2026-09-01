package com.security.adwaredetector.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.security.adwaredetector.MainActivity
import com.security.adwaredetector.R
import com.security.adwaredetector.model.RiskResult
import com.security.adwaredetector.receiver.NotificationActionReceiver

/**
 * Encapsula la creación del canal de notificaciones y el armado de la
 * notificación de alerta de alto riesgo, incluyendo la acción rápida
 * de "Desinstalar".
 */
object NotificationHelper {

    const val CHANNEL_ID = "adware_alert_channel"
    private const val CHANNEL_NAME = "Alertas de Adware"
    const val ACTION_UNINSTALL = "com.security.adwaredetector.ACTION_UNINSTALL"
    const val EXTRA_PACKAGE_NAME = "extra_package_name"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de alta prioridad para apps con riesgo de Adware"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Construye y dispara una notificación de alto riesgo para el resultado dado.
     * Incluye:
     *  - Un tap principal que abre la app en la pantalla de detalle/lista.
     *  - Un botón de acción "Desinstalar" que dispara NotificationActionReceiver.
     */
    fun showHighRiskAlert(context: Context, result: RiskResult) {
        createNotificationChannel(context)

        // Intent para abrir la app al tocar la notificación
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PACKAGE_NAME, result.packageName)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            result.packageName.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para la acción rápida "Desinstalar" (delegada a un BroadcastReceiver)
        val uninstallIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_UNINSTALL
            putExtra(EXTRA_PACKAGE_NAME, result.packageName)
        }
        val uninstallPendingIntent = PendingIntent.getBroadcast(
            context,
            result.packageName.hashCode() + 1,
            uninstallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reasonsText = result.reasons.joinToString(separator = " • ")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("⚠️ App de riesgo alto detectada: ${result.appName}")
            .setContentText("Puntaje de riesgo: ${result.score}/100")
            .setStyle(NotificationCompat.BigTextStyle().bigText(reasonsText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_delete, "Desinstalar", uninstallPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(result.packageName.hashCode(), notification)
    }

    /** Construye el Intent estándar del sistema para solicitar la desinstalación de un paquete. */
    fun buildUninstallIntent(packageName: String): Intent {
        return Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
