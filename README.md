# Barrio Vivo · MercaCerca

App Android nativa para encontrar el **tianguis más cercano** en la CDMX (1,325 tianguis del dataset del Gobierno de la Ciudad).
Universidad La Salle · Aplicaciones Móviles · Equipo MercaCerca.

## Stack
Kotlin · Jetpack Compose (Material 3) · MVVM + Repository · Hilt · Room (offline-first) · osmdroid (OpenStreetMap, sin API key) · Coroutines/Flow · Navigation-Compose.
minSdk 24 · targetSdk 35 · compileSdk 37 · AGP 9.3 · Gradle 9.5 · Kotlin 2.4.

## Cómo correrlo
1. Abre la carpeta en **Android Studio** (File › Open › `BarrioVivoApp`). Espera a que termine el *Gradle Sync*.
2. Conecta tu teléfono con depuración USB (abajo) o crea un emulador (Device Manager).
3. Botón ▶ **Run 'app'**.

Por terminal:
```powershell
./gradlew testDebugUnitTest     # pruebas unitarias
./gradlew installDebug          # instala en el teléfono conectado
./gradlew assembleRelease       # APK release con R8 (sin firmar)
```

### Samsung por USB
1. *Ajustes › Acerca del teléfono › Información de software* → toca **Número de compilación** 7 veces.
2. *Ajustes › Opciones de desarrollador* → activa **Depuración USB**.
3. Conecta por cable (de datos, no solo de carga), acepta **"¿Permitir depuración USB?"** en el teléfono.
4. El teléfono aparece en la barra superior de Android Studio → ▶ Run.

## Estructura
```
app/src/main/
├─ assets/tianguis_cdmx.json          # dataset (se carga a Room en el 1er arranque)
├─ java/com/mercacerca/barriovivo/
│  ├─ BarrioVivoApp.kt  MainActivity.kt
│  ├─ core/   geo (Haversine, Zócalo) · location (FusedLocation) · ui (cómo llegar, reduce-motion)
│  ├─ data/   local (Room: entities, DAOs, DB) · seed (parser JSON, seeder) · repository (impl)
│  ├─ di/     módulos Hilt
│  ├─ domain/ model (Tianguis, Dia, Testimonio) · repository (interfaces)
│  └─ ui/     theme · navigation · welcome · map · components · placeholder
└─ res/       font (Bebas Neue, DM Sans) · drawable-nodpi (arte de img/) · xml (network security)
data/04_seed_barriovivo.sql           # esquema de referencia + notas para Supabase (fase 2)
```

## Fases
- [x] **1** · Proyecto base, tema, Room + carga del JSON, Hilt, navegación, **Bienvenida** (comentarios flotantes) y **Mapa**
- [x] **2** · Ficha del tianguis (promedios + reseñas seed) + diálogo "¿Quieres ir?" + cómo llegar
- [x] **3** · Lista de cercanos (Haversine, min a pie) + "Mejor día" (radio 2 km)
- [ ] **4** · Registro/Login (PBKDF2 + salt) con estética de ticket + Perfil
- [ ] **5** · Evaluar (variedad/tamaño/calidad + de confianza) y favoritos
- [ ] **6** · Tus viajes (ticket + CO₂ × 0.17 kg/km) con el granjero + "¿Por qué ir al mercado?"
- [ ] **7** · (Opcional) Supabase: Auth + sync + RLS

## Seguridad
- Offline-first, sin backend en fase 1. Sin secretos en el código; `local.properties` y llaves fuera de git.
- Solo HTTPS (`network_security_config`, cleartext apagado). Room con consultas parametrizadas.
- Ubicación pedida solo al tocar "Cerca de mí"; si la niegan, el mapa se centra en el Zócalo.
- Release con R8 (minify + shrink resources).

## Créditos
Mapa © colaboradores de OpenStreetMap. Fuentes Bebas Neue y DM Sans (SIL Open Font License).
