# ϟ PanaBoost Pro 3.0 — Android

Centro de juegos Android desarrollado en Java 17 + Material Components, con datos reales del sistema.

## Funciones
- Pantalla gaming de diseño oscuro, cinco colores de acento.
- Biblioteca de aplicaciones instaladas, búsqueda, juegos identificados por Android, favoritos y acceso directo.
- Apertura de juegos y sesiones gaming voluntarias con historial local y opción para compartir.
- Información real de RAM libre/total, almacenamiento, batería, temperatura de batería, estado térmico y red.
- Gráfico en vivo del porcentaje de RAM utilizada (no FPS).
- Medición HTTPS al endpoint de prueba de Google (no es ping al servidor del juego).
- Estadísticas aproximadas de tiempo de uso con permiso específico concedido por el usuario.
- Modo No molestar opcional (necesita permiso explícito del sistema), restaura su estado anterior al finalizar sesión.
- Limpieza exclusiva de la caché propia y acceso a ajustes del sistema.

## Instalación sin conflicto con la versión anterior

El `applicationId` es **`com.panaboost.gamingpro`**, distinto de `com.panaboost.app` de las versiones anteriores. Esto evita `La app no se instaló debido a un conflicto con un paquete` provocado por firmas debug diferentes en el mismo package name. Ambos pueden coexistir.

## Compilación en GitHub Actions

1. Subir/descomprimir los archivos en la raíz del repositorio.
2. Ejecutar **Actions → Compilar PanaBoost Pro APK → Run workflow** (o esperar al push).
3. En la ejecución verde, descargar **PanaBoost-Pro-v3-APK**, descomprimir y abrir `app-debug.apk` en Android.

Se requiere Gradle 8.7, Java 17, Android SDK 35 y conexión a Maven/Google para Material Components.

## Límites y privacidad

- Android no permite que aplicaciones normales controlen frecuencias de CPU/GPU, fuercen FPS de los juegos, limpien caché de otras aplicaciones ni cierren todos los procesos. No inventamos porcentajes de mejora.
- Sin publicidad, sin cuentas, sin recolección de datos personales y sin servicios de seguimiento. El historial y favoritos se guardan localmente en SharedPreferences.
- Si activas la prueba de conexión, realiza una solicitud HTTPS a `https://www.google.com/generate_204`.
- El permiso de tiempo de uso es opcional y se solicita en Ajustes de Android.
- Desarrollado para Android 8.0+ (API 26). El comportamiento de algunos ajustes puede variar en HyperOS.

**Nota:** APK debug con firma de pruebas de GitHub Actions, adecuado para pruebas personales. Para publicar y actualizar de forma fiable en Play Store, configurar firma release privada con la misma clave en futuras versiones.
