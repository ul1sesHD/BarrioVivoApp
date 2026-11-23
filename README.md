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
│  ├─ core/   geo (Haversine, Zócalo) · location (FusedLocation) · security (PBKDF2, límite de intentos) · ui
│  ├─ data/   local (Room) · seed · repository · remote (Supabase / nube apagada) · sync (sincronizador)
│  ├─ di/     módulos Hilt
│  ├─ domain/ model (Tianguis, Dia, Review, Cuenta, Cercania, Viajes) · repository (interfaces)
│  └─ ui/     theme · navigation · welcome · map · cercanos · ficha · evaluar · cuenta · viajes · components
└─ res/       font (Bebas Neue, DM Sans) · drawable-nodpi (arte de img/) · xml (network security)
data/04_seed_barriovivo.sql           # esquema de referencia original
data/supabase/001_barrio_vivo.sql     # esquema de la nube (tablas + RLS + triggers)
```

## Fases
- [x] **1** · Proyecto base, tema, Room + carga del JSON, Hilt, navegación, **Bienvenida** (comentarios flotantes) y **Mapa**
- [x] **2** · Ficha del tianguis (promedios + reseñas seed) + diálogo "¿Quieres ir?" + cómo llegar
- [x] **3** · Lista de cercanos (Haversine, min a pie) + "Mejor día" (radio 2 km)
- [x] **4** · Registro/Login (PBKDF2 + salt, límite de intentos) con estética de ticket + Perfil
- [x] **5** · Evaluar (variedad/tamaño/calidad + de confianza, una por persona) y tianguis de confianza en el perfil
- [x] **6** · Tus viajes (ticket + CO₂ × 0.17 kg/km, ida y vuelta) con el granjero + "¿Por qué ir al mercado?"
- [x] **7** · Supabase (opcional): Auth con correo interno + sync de reseñas, favoritos y viajes + RLS

## Nube con Supabase (fase 7, opcional)
Sin llaves la app funciona igual, 100% offline. Con llaves, las reseñas se comparten entre todos y tus
favoritos y viajes se respaldan (y aparecen si entras desde otro teléfono).

1. Crea un proyecto en [supabase.com](https://supabase.com) (región más cercana, p. ej. `us-east-1`).
2. **Authentication → Sign In / Providers → Email**: déjalo activo y **apaga "Confirm email"**.
   La app no pide correo: convierte `marce` en `marce@users.barriovivo.invalid` (dominio reservado, nadie lo
   puede tener). Si Supabase lo rechazara, cambia solo `NubeRemota.correoInterno` por un dominio tuyo.
3. **SQL Editor → New query**: pega `data/supabase/001_barrio_vivo.sql` y dale **Run**.
4. **Project Settings → API**: copia *Project URL* y la llave **anon / publishable** a `local.properties`:
   ```properties
   SUPABASE_URL=https://xxxx.supabase.co
   SUPABASE_ANON_KEY=eyJ...
   ```
   (`local.properties` está en `.gitignore`. **Nunca** uses la `service_role` key en la app.)
5. *Sync Project with Gradle Files* y ▶ Run. Regístrate y revisa en **Table Editor → perfil** que apareció tu usuario.

Cómo funciona: Room sigue siendo la fuente de verdad. Al abrir la app, al entrar y después de evaluar, marcar
o sumar un viaje, el sincronizador baja las reseñas de todos y sube tu estado completo (es idempotente: si no hay
internet, se manda la siguiente vez). Si entras sin internet, se valida contra el hash local.

Límites conocidos: sin correo no hay "olvidé mi contraseña"; si alguien borra su reseña en la nube, en otros
teléfonos se queda hasta reinstalar.

## Seguridad
- Offline-first, sin backend en fase 1. Sin secretos en el código; `local.properties` y llaves fuera de git.
- Solo HTTPS (`network_security_config`, cleartext apagado). Room con consultas parametrizadas.
- Ubicación pedida solo al tocar "Cerca de mí"; si la niegan, el mapa se centra en el Zócalo.
- Release con R8 (minify + shrink resources).
- Contraseñas: PBKDF2-HMAC-SHA256 (310k iteraciones, salt de 16 bytes), comparación en tiempo constante, 5 intentos y 30 s de bloqueo.
- Nube: solo anon key + **RLS en todas las tablas**; `user_id` sale del token (`auth.uid()`), y los `CHECK` repiten las validaciones de la app.

## Créditos
Mapa © colaboradores de OpenStreetMap. Fuentes Bebas Neue y DM Sans (SIL Open Font License).
