package com.security.adwaredetector.ui

import androidx.core.graphics.drawable.toBitmap
import com.security.adwaredetector.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawable.toBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.security.adwaredetector.model.RiskLevel
import com.security.adwaredetector.model.RiskResult
import com.security.adwaredetector.notification.NotificationHelper

/**
 * Pantalla principal de auditoría manual: lista todas las apps de terceros
 * ordenadas por riesgo descendente, cada una con su badge de color y
 * un botón para desinstalar directamente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    onUninstallRequested: (String) -> Unit,
    viewModel: AppListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auditoría de Adware") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reescanear")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.results.isEmpty() -> {
                    Text(
                        text = "No se encontraron apps de terceros instaladas.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.results, key = { it.packageName }) { result ->
                            AppRiskCard(
                                result = result,
                                onUninstallClick = {
                                    onUninstallRequested(result.packageName)
                                    viewModel.removeFromList(result.packageName)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Tarjeta individual de una app con su ícono, badge de riesgo y razones detectadas. */
@Composable
fun AppRiskCard(
    result: RiskResult,
    onUninstallClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Ícono de la app (si se pudo cargar)
                result.icon?.let { drawable ->
                    Image(
                        bitmap = drawable.toBitmap().asImageBitmap(),
                        contentDescription = result.appName,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = result.packageName,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                RiskBadge(level = result.level, score = result.score)
            }

            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Motivos detectados:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                result.reasons.forEach { reason ->
                    Text(text = "• $reason", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onUninstallClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Desinstalar")
                }
            }
        }
    }
}

/** Badge de color según el nivel de riesgo: Verde (bajo), Amarillo (medio), Rojo (alto). */
@Composable
fun RiskBadge(level: RiskLevel, score: Int) {
    val color = when (level) {
        RiskLevel.LOW -> Color(0xFF2E7D32)     // Verde
        RiskLevel.MEDIUM -> Color(0xFFF9A825)  // Amarillo
        RiskLevel.HIGH -> Color(0xFFC62828)    // Rojo
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "${level.label} ($score)",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// Import necesario para el composable Image (se separa para claridad de scope)
@Composable
private fun Image(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier
    )
}
