-- =====================================================================
-- Barrio Vivo · Esquema + datos SEED  (SQLite / Room)
-- Equipo MercaCerca (app: Barrio Vivo) · Universidad La Salle · Aplicaciones Móviles
-- ---------------------------------------------------------------------
-- Uso:
--   * Los 1,325 tianguis NO se insertan aquí: la app los carga desde
--     assets/tianguis_cdmx.json a Room en el primer arranque (más limpio
--     que 1,325 INSERT). Este archivo crea el esquema y mete los datos
--     SEED de evaluaciones, favoritos, perfil y testimonios.
--   * Sirve igual para SQLite (Room, bundleado en el cel) y, con cambios
--     mínimos de tipos, para Postgres/Supabase (ver notas al final).
-- =====================================================================

PRAGMA foreign_keys = ON;

-- ---- Catálogo de tianguis (se llena desde el JSON, se deja el DDL) ---
CREATE TABLE IF NOT EXISTS tianguis (
  id         INTEGER PRIMARY KEY,
  dia        TEXT    NOT NULL,          -- LUNES..DOMINGO
  nombre     TEXT,                      -- nombre del tianguis si lo tiene
  ubicacion  TEXT    NOT NULL,
  hora_ini   TEXT,                      -- "08:00"
  hora_fin   TEXT,                      -- "19:00"
  lat        REAL,
  lon        REAL,
  nota       TEXT
);
CREATE INDEX IF NOT EXISTS idx_tianguis_dia ON tianguis(dia);

-- ---- Usuarios locales (usuario + contraseña HASHEADA) ----------------
-- Seguridad: NUNCA se guarda la contraseña en texto plano. La app hashea
-- con PBKDF2WithHmacSHA256 + salt aleatorio por usuario y guarda solo
-- password_hash y password_salt. Avatar = ícono de persona con color.
CREATE TABLE IF NOT EXISTS usuario (
  id            INTEGER PRIMARY KEY,
  usuario       TEXT    NOT NULL UNIQUE,
  password_hash TEXT    NOT NULL,       -- PBKDF2 (nunca texto plano)
  password_salt TEXT    NOT NULL,       -- salt aleatorio por usuario
  color_icono   TEXT    NOT NULL DEFAULT '#D90368',  -- color del avatar
  creado_en     TEXT DEFAULT (datetime('now'))
);

-- ---- Evaluaciones: 3 preguntas + "tianguis de confianza" -------------
CREATE TABLE IF NOT EXISTS review (
  id          INTEGER PRIMARY KEY,
  tianguis_id INTEGER NOT NULL REFERENCES tianguis(id) ON DELETE CASCADE,
  autor       TEXT    NOT NULL,
  variedad    INTEGER NOT NULL CHECK (variedad BETWEEN 1 AND 5),
  tamano      INTEGER NOT NULL CHECK (tamano   BETWEEN 1 AND 5),
  calidad     INTEGER NOT NULL CHECK (calidad  BETWEEN 1 AND 5),
  confianza   INTEGER NOT NULL DEFAULT 0 CHECK (confianza IN (0,1)),
  comentario  TEXT,
  creado_en   TEXT DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_review_tianguis ON review(tianguis_id);

-- ---- Favoritos / "tianguis de confianza" por perfil ------------------
CREATE TABLE IF NOT EXISTS favorito (
  tianguis_id INTEGER NOT NULL REFERENCES tianguis(id) ON DELETE CASCADE,
  creado_en   TEXT DEFAULT (datetime('now')),
  PRIMARY KEY (tianguis_id)
);

-- ---- "Tus viajes": CO2 ahorrado al caminar al mercado ----------------
-- Factor sugerido: 0.17 kg CO2e por km (auto gasolina promedio). Ajústalo.
CREATE TABLE IF NOT EXISTS viaje (
  id            INTEGER PRIMARY KEY,
  tianguis_id   INTEGER REFERENCES tianguis(id) ON DELETE SET NULL,
  distancia_km  REAL    NOT NULL,
  co2_kg        REAL    NOT NULL,       -- distancia_km * 0.17
  fecha         TEXT DEFAULT (datetime('now'))
);

-- ---- Testimonios de la pantalla de bienvenida ------------------------
CREATE TABLE IF NOT EXISTS testimonio (
  id      INTEGER PRIMARY KEY,
  autor   TEXT NOT NULL,
  texto   TEXT NOT NULL
);

-- =====================================================================
-- DATOS SEED
-- =====================================================================

-- Usuarios demo. password_hash/salt son PLACEHOLDERS: en la app, al
-- registrarse se generan con PBKDF2. Estos sirven solo para ver la UI;
-- la contraseña demo de todos es "mercado123" (regenérala con tu hasher).
INSERT INTO usuario (id, usuario, password_hash, password_salt, color_icono) VALUES
  (1, 'marce',   'REEMPLAZAR_CON_PBKDF2', 'REEMPLAZAR_SALT', '#D90368'),
  (2, 'donbeto', 'REEMPLAZAR_CON_PBKDF2', 'REEMPLAZAR_SALT', '#5079B2'),
  (3, 'sofia',   'REEMPLAZAR_CON_PBKDF2', 'REEMPLAZAR_SALT', '#FC2F00');

-- Evaluaciones seed (ids de tianguis reales del dataset limpio)
INSERT INTO review (id, tianguis_id, autor, variedad, tamano, calidad, confianza, comentario) VALUES
  (1, 90, 'Luis', 3, 5, 5, 1, 'Muy surtido de fruta y verdura, llegas temprano y está fresquísimo.'),
  (2, 90, 'Ana', 5, 2, 4, 1, 'Chico pero bien surtido, atención de diez.'),
  (3, 94, 'Kari', 3, 2, 3, 0, 'Pasillo amplio, fácil de recorrer con el carrito.'),
  (4, 94, 'Kari', 3, 3, 3, 0, 'Calidad excelente en verduras, repito cada semana.'),
  (5, 94, 'Ana', 3, 2, 3, 0, 'Chico pero bien surtido, atención de diez.'),
  (6, 107, 'Ana', 5, 5, 3, 1, 'Precios justos y producto de temporada muy bueno.'),
  (7, 107, 'Marce', 5, 3, 4, 1, 'Pasillo amplio, fácil de recorrer con el carrito.'),
  (8, 107, 'Pau', 5, 2, 5, 1, 'Un poco lleno a mediodía pero vale la pena.'),
  (9, 111, 'Diego', 3, 2, 5, 1, 'Chico pero bien surtido, atención de diez.'),
  (10, 111, 'Rodo', 4, 2, 5, 1, 'Buen ambiente, los marchantes te dan buen trato.'),
  (11, 111, 'Kari', 3, 3, 4, 0, 'Calidad excelente en verduras, repito cada semana.'),
  (12, 139, 'Diego', 4, 5, 4, 1, 'Un poco lleno a mediodía pero vale la pena.'),
  (13, 139, 'Sofía', 3, 3, 3, 0, 'Chico pero bien surtido, atención de diez.'),
  (14, 154, 'Sofía', 4, 4, 5, 1, 'Encontré cosas que no hay en el súper, muy local.'),
  (15, 154, 'Kari', 5, 2, 3, 1, 'Calidad excelente en verduras, repito cada semana.'),
  (16, 154, 'Kari', 3, 4, 3, 0, 'Encontré cosas que no hay en el súper, muy local.'),
  (17, 160, 'Beto', 5, 2, 5, 1, 'Chico pero bien surtido, atención de diez.'),
  (18, 175, 'Rodo', 5, 4, 5, 1, 'Encontré cosas que no hay en el súper, muy local.'),
  (19, 175, 'Memo', 4, 2, 3, 0, 'Un poco lleno a mediodía pero vale la pena.'),
  (20, 231, 'Rodo', 5, 2, 3, 1, 'Un poco lleno a mediodía pero vale la pena.'),
  (21, 231, 'Beto', 5, 5, 4, 1, 'Pasillo amplio, fácil de recorrer con el carrito.'),
  (22, 231, 'Rodo', 3, 5, 4, 0, 'Variedad enorme, encuentras de todo en un solo lugar.'),
  (23, 233, 'Marce', 4, 2, 3, 0, 'Un poco lleno a mediodía pero vale la pena.'),
  (24, 328, 'Luis', 3, 5, 4, 0, 'Encontré cosas que no hay en el súper, muy local.'),
  (25, 328, 'Sofía', 3, 5, 4, 0, 'Calidad excelente en verduras, repito cada semana.'),
  (26, 328, 'Kari', 3, 5, 5, 1, 'Un poco lleno a mediodía pero vale la pena.'),
  (27, 359, 'Luis', 5, 5, 3, 1, 'Variedad enorme, encuentras de todo en un solo lugar.'),
  (28, 359, 'Ana', 3, 3, 3, 0, 'Precios justos y producto de temporada muy bueno.'),
  (29, 360, 'Ana', 5, 3, 4, 1, 'Un poco lleno a mediodía pero vale la pena.'),
  (30, 360, 'Rodo', 3, 5, 5, 1, 'La comida preparada está deliciosa, recomendado.'),
  (31, 430, 'Rodo', 4, 3, 5, 1, 'Calidad excelente en verduras, repito cada semana.'),
  (32, 430, 'Kari', 5, 2, 4, 1, 'Calidad excelente en verduras, repito cada semana.'),
  (33, 430, 'Memo', 4, 5, 4, 1, 'Buen ambiente, los marchantes te dan buen trato.'),
  (34, 431, 'Diego', 4, 2, 3, 0, 'Buen ambiente, los marchantes te dan buen trato.'),
  (35, 431, 'Rodo', 4, 3, 3, 0, 'La comida preparada está deliciosa, recomendado.'),
  (36, 431, 'Marce', 3, 2, 3, 0, 'Chico pero bien surtido, atención de diez.'),
  (37, 449, 'Luis', 3, 4, 5, 1, 'Muy surtido de fruta y verdura, llegas temprano y está fresquísimo.'),
  (38, 449, 'Beto', 3, 5, 3, 0, 'Un poco lleno a mediodía pero vale la pena.'),
  (39, 449, 'Luis', 5, 4, 4, 1, 'Buen ambiente, los marchantes te dan buen trato.'),
  (40, 486, 'Luis', 4, 5, 4, 1, 'Un poco lleno a mediodía pero vale la pena.'),
  (41, 486, 'Sofía', 3, 2, 5, 1, 'La comida preparada está deliciosa, recomendado.'),
  (42, 490, 'Diego', 5, 3, 5, 1, 'Muy surtido de fruta y verdura, llegas temprano y está fresquísimo.'),
  (43, 490, 'Ana', 5, 4, 3, 1, 'Calidad excelente en verduras, repito cada semana.'),
  (44, 505, 'Pau', 4, 2, 5, 1, 'Un poco lleno a mediodía pero vale la pena.'),
  (45, 505, 'Pau', 4, 3, 4, 1, 'Precios justos y producto de temporada muy bueno.'),
  (46, 505, 'Rodo', 5, 4, 5, 1, 'Precios justos y producto de temporada muy bueno.'),
  (47, 521, 'Diego', 3, 5, 5, 1, 'Precios justos y producto de temporada muy bueno.');

-- Un par de tianguis marcados como "de confianza" de ejemplo
INSERT INTO favorito (tianguis_id) VALUES (90), (505), (486);

-- Testimonios para la bienvenida ("gracias por barrio vivo")
INSERT INTO testimonio (id, autor, texto) VALUES
  (1, 'Marce', 'Gracias a MercaCerca encontré el tianguis de mi colonia, ahora compro local cada semana.'),
  (2, 'Don Beto', 'Soy marchante y llegó gente nueva que ni sabía que estábamos aquí. ¡Barrio vivo!'),
  (3, 'Sofía', 'Me mudé hace poco y en un toque supe qué día se pone el tianguis cerca de mi casa.'),
  (4, 'Memo', 'Me encanta ver cuánto CO2 ahorro caminando al mercado. Enganchadísimo.'),
  (5, 'Kari', 'La fruta más fresca y barata de mi zona, y apoyo a los de aquí.');

-- =====================================================================
-- NOTAS PARA SUPABASE / POSTGRES (fase 2, opcional)
-- ---------------------------------------------------------------------
-- 1) INTEGER PRIMARY KEY -> BIGINT GENERATED ALWAYS AS IDENTITY.
-- 2) datetime('now')     -> now()  (columnas timestamptz default now()).
-- 3) confianza INTEGER 0/1 -> BOOLEAN.
-- 4) Seguridad: activa Row Level Security en review/favorito/viaje.
--      - SELECT público en tianguis y review (lectura abierta).
--      - INSERT/UPDATE/DELETE solo del dueño: auth.uid() = user_id.
--    NUNCA pongas la service_key en la app; usa solo la anon key + RLS.
-- =====================================================================
