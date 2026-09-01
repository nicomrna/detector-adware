package com.security.adwaredetector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.security.adwaredetector.notification.NotificationHelper
import com.security.adwaredetector.ui.AppListScreen

/**
 * Activity única de la app. Responsabilidades:
 *  1. Solicitar el permiso POST_NOTIFICATIONS (obligatorio desde Android 13).
 *  2. Crear el canal de notificaciones.
 *  3. Hostear la pantalla Compose de auditoría manual (AppListScreen).
 *  4. Al pedir desinstalar una app desde la lista, lanzar el Intent nativo del sistema.
 */
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* No-op: si el usuario lo niega, simplemente no habrá alertas push */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)
        ensureNotificationPermission()

        setContent {
            AdwareDetectorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppListScreen(
                        onUninstallRequested = { packageName ->
                            startActivity(NotificationHelper.buildUninstallIntent(packageName))
                        }
                    )
                }
            }
        }
    }

    /** En Android 13+ las notificaciones requieren permiso explícito en runtime. */
    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun AdwareDetectorTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
