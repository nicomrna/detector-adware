package com.security.adwaredetector.model

import android.graphics.drawable.Drawable

/**
 * Nivel de riesgo categorizado a partir del puntaje numérico.
 * Se usa para pintar el badge de color en la UI (Verde/Amarillo/Rojo).
 */
enum class RiskLevel(val label: String) {
    LOW("Bajo"),
    MEDIUM("Medio"),
    HIGH("Alto");

    companion object {
        /**
         * Convierte un puntaje 0-100 en su categoría correspondiente.
         * < 30  -> LOW
         * 30-59 -> MEDIUM
         * >= 60 -> HIGH
         */
        fun fromScore(score: Int): RiskLevel = when {
            score >= 60 -> HIGH
            score >= 30 -> MEDIUM
            else -> LOW
        }
    }
}

/**
 * Resultado del análisis de una app: puntaje total, nivel derivado
 * y la lista de "razones" (hallazgos) que explican por qué se le asignó ese puntaje.
 */
data class RiskResult(
    val packageName: String,
    val appName: String,
    val score: Int,
    val reasons: List<String>,
    val isSystemApp: Boolean,
    val icon: Drawable? = null
) {
    val level: RiskLevel get() = RiskLevel.fromScore(score)
}
