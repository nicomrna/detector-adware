# Adware Detector (Android / Kotlin / Jetpack Compose)

App de auditoría y alerta preventiva de apps con posibles características de Adware.

## Cómo abrir el proyecto
1. Abrí Android Studio (Koala o superior recomendado).
2. `File > Open` y seleccioná la carpeta `AdwareDetector/`.
3. Dejá que Gradle sincronice (usa AGP 8.5.2 / Kotlin 1.9.24 / compileSdk 34).
4. Ejecutá en un emulador o dispositivo con **minSdk 26** (Android 8.0+).

## Estructura del proyecto
```
app/src/main/
├── AndroidManifest.xml
├── java/com/security/adwaredetector/
│   ├── MainActivity.kt                 -> Host de la UI Compose
│   ├── model/RiskResult.kt             -> Modelo de datos del análisis
│   ├── scanner/AdwareScanner.kt        -> Algoritmo de puntaje de riesgo
│   ├── receiver/InstallReceiver.kt     -> Escucha PACKAGE_ADDED (tiempo real)
│   ├── receiver/NotificationActionReceiver.kt -> Botón "Desinstalar" de la notif.
│   ├── notification/NotificationHelper.kt     -> Construcción de notificaciones
│   └── ui/AppListScreen.kt, AppListViewModel.kt -> Pantalla de auditoría manual
└── res/...
```

## Algoritmo de riesgo (0-100)
| Señal detectada                                   | Puntos |
|----------------------------------------------------|--------|
| Permiso `SYSTEM_ALERT_WINDOW` (overlay/pop-ups)     | +35    |
| Sin ícono en el Launcher                            | +30    |
| `RECEIVE_BOOT_COMPLETED` (persistencia al bootear)  | +15    |
| Declara un `AccessibilityService`                   | +20    |
| App de sistema (`FLAG_SYSTEM`)                      | Score forzado a 0 (excluida) |

Categorías: **Bajo** (<30, verde) · **Medio** (30-59, amarillo) · **Alto** (≥60, rojo).

## Notas técnicas y limitaciones importantes (léelas antes de usar en producción)

1. **`QUERY_ALL_PACKAGES`**: para poder listar y analizar *todas* las apps de
   terceros en Android 11+, se declara este permiso en el manifiesto. Si publicás
   la app en Google Play, tendrás que justificar su uso en la Play Console
   (las apps de seguridad/antivirus están explícitamente permitidas para esto).
   Como alternativa más restrictiva, se puede usar `<queries>` con intents
   específicos, pero eso limita qué paquetes son visibles.

2. **Desinstalación**: Android no permite que una app normal (no privilegiada
   ni "device owner") desinstale otra app de forma silenciosa. Por diseño de
   seguridad, `Intent.ACTION_DELETE` siempre abre el diálogo nativo de
   confirmación del sistema. El código ya usa ese enfoque, tanto desde la
   lista (`MainActivity`) como desde la acción de la notificación
   (`NotificationActionReceiver`).

3. **Heurística, no certeza**: el puntaje es una heurística basada en
   permisos/atributos declarados en el manifiesto de cada app. Puede haber
   falsos positivos (ej. apps legítimas de accesibilidad, lanzadores de
   overlays para funciones válidas como "burbujas" de chat, etc.). El diseño
   de la UI (`RiskResult.reasons`) siempre muestra el motivo detectado para
   que el usuario tome la decisión final con contexto.

4. **`ACTION_PACKAGE_ADDED` en segundo plano**: a partir de Android 8 (Oreo),
   los *manifest-declared broadcast receivers* implícitos están muy
   restringidos, pero `PACKAGE_ADDED` es una de las excepciones explícitas que
   el sistema sigue entregando a receivers declarados estáticamente en el
   manifiesto (junto con `BOOT_COMPLETED`, `PACKAGE_REPLACED`, etc.), por lo
   que este enfoque sigue siendo válido y no requiere un foreground service.

5. **Permiso de notificaciones (Android 13+)**: `MainActivity` solicita
   `POST_NOTIFICATIONS` en runtime. Si el usuario lo rechaza, el escáner
   manual seguirá funcionando con normalidad; solo se pierde la alerta
   push en tiempo real.

## Próximos pasos sugeridos
- Persistir resultados con Room para no re-analizar todo en cada apertura.
- Agregar una allowlist configurable por el usuario (apps de confianza).
- Sumar más señales heurísticas (ej. `BIND_DEVICE_ADMIN`, número de actividades
  ocultas, certificado de firma no reconocido, etc.).
