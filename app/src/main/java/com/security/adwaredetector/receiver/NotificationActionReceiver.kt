package com.security.adwaredetector.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.security.adwaredetector.notification.NotificationHelper

/**
 * Receiver liviano que atiende el botón "Desinstalar" de la notificación.
 *
 * Importante: Android NO permite desinstalar un paquete de forma silenciosa
 * desde una app normal (no privilegiada) sin confirmación del usuario.
 * Por eso, lo que hacemos es lanzar el Intent.ACTION_DELETE del sistema,
 * que abre el diálogo nativo de desinstalación con el paquete ya preseleccionado,
 * minimizando la fricción para el usuario.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationHelper.ACTION_UNINSTALL) return

        val packageName = intent.getStringExtra(NotificationHelper.EXTRA_PACKAGE_NAME) ?: return

        // Lanzamos el diálogo nativo de desinstalación
        val uninstallIntent = NotificationHelper.buildUninstallIntent(packageName)
        context.startActivity(uninstallIntent)

        // Cerramos la notificación ya que la acción fue iniciada
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(packageName.hashCode())
    }
}
