-- =====================================================================
-- Barrio Vivo · Fase 7 · Esquema en Supabase (Postgres) con RLS
-- Ejecútalo UNA vez en: Supabase → SQL Editor → New query → Run.
-- ---------------------------------------------------------------------
-- Qué vive en la nube:
--   perfil    usuario + color del ícono (público: se ve junto a las reseñas)
--   review    evaluaciones (PÚBLICAS para leer; cada quien edita las suyas)
--   favorito  "mis tianguis de confianza" (PRIVADO)
--   viaje     ticket de viajes / CO2 (PRIVADO)
-- El catálogo de 1,325 tianguis NO se sube: viene en la app (assets).
--
-- Seguridad:
--   * Row Level Security activado en TODAS las tablas.
--   * La app solo usa la anon key; la service_role key NUNCA va en la app.
--   * user_id sale de auth.uid() (el token), no de lo que mande el cliente.
--   * Los CHECK repiten las reglas de la app: el servidor no confía en el cliente.
-- =====================================================================

-- ---------- perfil ----------------------------------------------------
create table if not exists public.perfil (
  id          uuid primary key references auth.users (id) on delete cascade,
  usuario     text not null unique check (usuario ~ '^[a-z0-9_.]{3,20}$'),
  color_icono text not null default '#D90368' check (color_icono ~ '^#[0-9A-F]{6}$'),
  creado_en   timestamptz not null default now()
);

-- El perfil se crea solo al registrarse (con los datos que manda la app en el signUp).
create or replace function public.crear_perfil()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
  insert into public.perfil (id, usuario, color_icono)
  values (
    new.id,
    lower(new.raw_user_meta_data ->> 'usuario'),
    coalesce(upper(new.raw_user_meta_data ->> 'color'), '#D90368')
  );
  return new;
end;
$$;

drop trigger if exists al_registrar_usuario on auth.users;
create trigger al_registrar_usuario
  after insert on auth.users
  for each row execute function public.crear_perfil();

-- ---------- review ----------------------------------------------------
create table if not exists public.review (
  id             uuid primary key default gen_random_uuid(),
  user_id        uuid not null default auth.uid() references auth.users (id) on delete cascade,
  tianguis_id    integer not null check (tianguis_id > 0),
  variedad       smallint not null check (variedad between 1 and 5),
  tamano         smallint not null check (tamano between 1 and 5),
  calidad        smallint not null check (calidad between 1 and 5),
  confianza      boolean not null default false,
  comentario     text check (char_length(comentario) <= 280),
  actualizado_en timestamptz not null default now(),
  unique (user_id, tianguis_id)            -- una evaluación por persona y tianguis
);
create index if not exists review_tianguis_idx on public.review (tianguis_id);

-- Lectura pública con el nombre y color del autor (respeta el RLS de quien consulta).
create or replace view public.review_publica
with (security_invoker = true) as
  select r.id, r.user_id, r.tianguis_id, r.variedad, r.tamano, r.calidad,
         r.confianza, r.comentario, r.actualizado_en,
         p.usuario as autor, p.color_icono
  from public.review r
  join public.perfil p on p.id = r.user_id;

-- ---------- favorito --------------------------------------------------
create table if not exists public.favorito (
  user_id     uuid not null default auth.uid() references auth.users (id) on delete cascade,
  tianguis_id integer not null check (tianguis_id > 0),
  creado_en   timestamptz not null default now(),
  primary key (user_id, tianguis_id)
);

-- ---------- viaje -----------------------------------------------------
create table if not exists public.viaje (
  id              uuid primary key,                 -- lo genera la app (sincroniza sin duplicar)
  user_id         uuid not null default auth.uid() references auth.users (id) on delete cascade,
  tianguis_id     integer check (tianguis_id > 0),
  tianguis_titulo text not null check (char_length(tianguis_titulo) <= 200),
  distancia_km    numeric(6, 2) not null check (distancia_km between 0 and 20),
  co2_kg          numeric(7, 3) not null check (co2_kg >= 0),
  fecha           timestamptz not null
);
create index if not exists viaje_user_idx on public.viaje (user_id);

-- =====================================================================
-- ROW LEVEL SECURITY
-- (select auth.uid()) en lugar de auth.uid(): Postgres lo evalúa una vez por consulta.
-- =====================================================================
alter table public.perfil   enable row level security;
alter table public.review   enable row level security;
alter table public.favorito enable row level security;
alter table public.viaje    enable row level security;

-- perfil: todos leen (nombre + color junto a las reseñas); cada quien edita el suyo.
drop policy if exists "perfil lectura publica" on public.perfil;
create policy "perfil lectura publica" on public.perfil
  for select to anon, authenticated using (true);
drop policy if exists "perfil editar propio" on public.perfil;
create policy "perfil editar propio" on public.perfil
  for update to authenticated
  using ((select auth.uid()) = id) with check ((select auth.uid()) = id);
-- (sin policy de insert/delete: solo el trigger crea perfiles; se borran en cascada)

-- review: lectura pública; escribir/editar/borrar solo las propias.
drop policy if exists "review lectura publica" on public.review;
create policy "review lectura publica" on public.review
  for select to anon, authenticated using (true);
drop policy if exists "review crear propia" on public.review;
create policy "review crear propia" on public.review
  for insert to authenticated with check ((select auth.uid()) = user_id);
drop policy if exists "review editar propia" on public.review;
create policy "review editar propia" on public.review
  for update to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
drop policy if exists "review borrar propia" on public.review;
create policy "review borrar propia" on public.review
  for delete to authenticated using ((select auth.uid()) = user_id);

-- favorito y viaje: 100% privados.
drop policy if exists "favorito solo dueño" on public.favorito;
create policy "favorito solo dueño" on public.favorito
  for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
drop policy if exists "viaje solo dueño" on public.viaje;
create policy "viaje solo dueño" on public.viaje
  for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);

-- Que el usuario no pueda "regalarse" reseñas cambiando la fecha: la pone el servidor.
create or replace function public.tocar_actualizado()
returns trigger language plpgsql set search_path = '' as $$
begin
  new.actualizado_en := now();
  return new;
end;
$$;
drop trigger if exists review_actualizado on public.review;
create trigger review_actualizado before insert or update on public.review
  for each row execute function public.tocar_actualizado();
