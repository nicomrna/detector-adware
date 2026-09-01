package com.security.adwaredetector.scanner

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.security.adwaredetector.model.RiskResult

/**
 * Motor de análisis heurístico para detectar señales típicas de Adware.
 *
 * IMPORTANTE: Este scanner NO afirma con certeza que una app sea maliciosa.
 * Se basa en heurísticas de permisos y comportamiento declarado en el manifiesto,
 * comunes en apps de tipo adware (pop-ups, ejecución oculta, persistencia).
 * El resultado debe presentarse al usuario como una alerta de riesgo, no como
 * un veredicto definitivo, y siempre dejando la decisión final (desinstalar o no)
 * en manos del usuario.
 */
class AdwareScanner(private val context: Context) {

    companion object {
        private const val TAG = "AdwareScanner"

        // --- Pesos del algoritmo de puntaje (0-100) ---
        const val SCORE_OVERLAY_PERMISSION = 35   // SYSTEM_ALERT_WINDOW
        const val SCORE_NO_LAUNCHER_ICON = 30     // Sin ícono visible en el launcher
        const val SCORE_BOOT_PERSISTENCE = 15     // RECEIVE_BOOT_COMPLETED
        const val SCORE_ACCESSIBILITY_SERVICE = 20 // Declara un AccessibilityService

        const val MAX_SCORE = 100
    }

    private val packageManager: PackageManager = context.packageManager

    /**
     * Analiza un único paquete a partir de su nombre.
     * Retorna null si el paquete no existe o no pudo consultarse.
     */
    fun analyzePackage(packageName: String): RiskResult? {
        return try {
            val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_RECEIVERS or PackageManager.GET_SERVICES
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(flags.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, flags)
            }
            analyze(packageInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Paquete no encontrado: $packageName")
            null
        }
    }

    /**
     * Punto de entrada principal del algoritmo: recibe un PackageInfo completo
     * (con permisos, receivers y services) y devuelve el resultado del análisis.
     */
    fun analyze(packageInfo: PackageInfo): RiskResult {
        val appInfo: ApplicationInfo = packageInfo.applicationInfo
            ?: return emptyResult(packageInfo.packageName)

        val appName = packageManager.getApplicationLabel(appInfo).toString()
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        // Regla de negocio: las apps de sistema quedan excluidas del análisis (score 0).
        if (isSystemApp) {
            return RiskResult(
                packageName = packageInfo.packageName,
                appName = appName,
                score = 0,
                reasons = listOf("Aplicación del sistema (excluida del análisis)"),
                isSystemApp = true,
                icon = safeIcon(appInfo)
            )
        }

        var score = 0
        val reasons = mutableListOf<String>()

        // 1) SYSTEM_ALERT_WINDOW -> permiso clave usado para pop-ups sobre otras apps
        if (hasPermission(packageInfo, android.Manifest.permission.SYSTEM_ALERT_WINDOW)) {
            score += SCORE_OVERLAY_PERMISSION
            reasons.add("Tiene permiso para dibujar sobre otras apps (posibles pop-ups)")
        }

        // 2) Sin ícono en el launcher -> técnica común de ocultamiento
        if (packageManager.getLaunchIntentForPackage(packageInfo.packageName) == null) {
            score += SCORE_NO_LAUNCHER_ICON
            reasons.add("No tiene ícono visible en el launcher (posible ocultamiento)")
        }

        // 3) RECEIVE_BOOT_COMPLETED -> se ejecuta automáticamente al encender el dispositivo
        if (hasPermission(packageInfo, android.Manifest.permission.RECEIVE_BOOT_COMPLETED)) {
            score += SCORE_BOOT_PERSISTENCE
            reasons.add("Se ejecuta automáticamente al iniciar el sistema")
        }

        // 4) Declara un AccessibilityService -> puede leer pantalla/interactuar con otras apps
        if (declaresAccessibilityService(packageInfo)) {
            score += SCORE_ACCESSIBILITY_SERVICE
            reasons.add("Declara un servicio de Accesibilidad (puede monitorear/controlar la pantalla)")
        }

        val finalScore = score.coerceAtMost(MAX_SCORE)

        if (reasons.isEmpty()) {
            reasons.add("No se detectaron señales de riesgo relevantes")
        }

        return RiskResult(
            packageName = packageInfo.packageName,
            appName = appName,
            score = finalScore,
            reasons = reasons,
            isSystemApp = false,
            icon = safeIcon(appInfo)
        )
    }

    /** Analiza en bloque todas las apps de terceros instaladas en el dispositivo. */
    fun scanAllInstalledApps(): List<RiskResult> {
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_RECEIVERS or PackageManager.GET_SERVICES
        val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(flags)
        }

        return packages
            .mapNotNull { pkg ->
                try {
                    analyze(pkg)
                } catch (e: Exception) {
                    Log.e(TAG, "Error analizando ${pkg.packageName}", e)
                    null
                }
            }
            // Ocultamos apps de sistema de la lista de auditoría manual
            .filterNot { it.isSystemApp }
            // Orden descendente por puntaje de riesgo, tal como pide el requisito
            .sortedByDescending { it.score }
    }

    // ---------------------------------------------------------------------
    // Helpers internos
    // ---------------------------------------------------------------------

    private fun hasPermission(packageInfo: PackageInfo, permission: String): Boolean {
        val requested = packageInfo.requestedPermissions ?: return false
        return requested.any { it == permission }
    }

    /**
     * Recorre los <service> declarados en el manifiesto buscando alguno que
     * extienda o esté asociado a AccessibilityService, verificando el permiso
     * BIND_ACCESSIBILITY_SERVICE que el sistema exige para este tipo de servicios.
     */
    private fun declaresAccessibilityService(packageInfo: PackageInfo): Boolean {
        val services = packageInfo.services ?: return false
        return services.any { serviceInfo ->
            serviceInfo.permission == android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE
        }
    }

    private fun safeIcon(appInfo: ApplicationInfo) = try {
        appInfo.loadIcon(packageManager)
    } catch (e: Exception) {
        null
    }

    private fun emptyResult(packageName: String) = RiskResult(
        packageName = packageName,
        appName = packageName,
        score = 0,
        reasons = listOf("No se pudo obtener información de la app"),
        isSystemApp = false,
        icon = null
    )
}
