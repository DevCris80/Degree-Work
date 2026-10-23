-- Tablas centrales de la finca (SRS v0.2, ADR-0004, ADR-0005).
--
-- Replican las entidades de Room con tres diferencias: no llevan `sincronizado`, `rev_base` ni `id_version`
-- (son estado de cada celular); las fechas son timestamptz en lugar de milisegundos; y cada tabla lleva
-- `cursor_descarga`, que toma un valor nuevo de una secuencia global en cada escritura para la descarga
-- incremental (RF-36). Las tablas hijas llevan además `id_perfil_finca`, que llena un trigger, para filtrar
-- por finca sin unir con `animal`.
--
-- Los textos de sexo, etapa, propósito, rol, motivo y resolución son los mismos que guarda Room.

create schema privado;
grant usage on schema privado to authenticated;

create sequence privado.cursor_descarga;
grant usage on sequence privado.cursor_descarga to authenticated;

create function privado.asignar_cursor_descarga() returns trigger
language plpgsql set search_path = '' as $$
begin
  new.cursor_descarga := nextval('privado.cursor_descarga');
  return new;
end;
$$;

create table public.perfil_finca (
  id_perfil_finca uuid primary key,
  nombre_finca text not null,
  latitud double precision,
  longitud double precision,
  fecha_modificacion timestamptz not null,
  cursor_descarga bigint not null
);

create table public.usuario (
  id_usuario uuid primary key references auth.users (id),
  id_perfil_finca uuid not null references public.perfil_finca,
  nombre text not null,
  rol text not null check (rol in ('Ganadero', 'Operario')),
  fecha_modificacion timestamptz not null,
  cursor_descarga bigint not null
);
create index on public.usuario (id_perfil_finca);

create table public.animal (
  id_animal uuid primary key,
  id_perfil_finca uuid not null references public.perfil_finca,
  id_padre uuid references public.animal,
  id_madre uuid references public.animal,
  nombre text not null,
  raza text not null,
  sexo text not null check (sexo in ('Macho', 'Hembra')),
  etapa text not null,
  fecha_nacimiento timestamptz not null,
  fecha_nacimiento_es_estimada boolean not null default false,
  proposito text not null check (proposito in ('Leche', 'Carne', 'Doble Propósito')),
  fecha_baja timestamptz,
  fecha_modificacion timestamptz not null,
  rev integer not null default 1 check (rev >= 1),
  cursor_descarga bigint not null,
  -- RN-03
  constraint etapa_del_sexo check (
    (sexo = 'Macho' and etapa in ('Toro', 'Novillo')) or (sexo = 'Hembra' and etapa in ('Vaca', 'Ternera'))
  )
);
create index on public.animal (id_perfil_finca);

create table public.evento (
  id_evento uuid primary key,
  id_animal uuid not null references public.animal,
  id_usuario uuid not null references public.usuario,
  id_perfil_finca uuid not null references public.perfil_finca,
  -- Los valores quedan por completar hasta que llegue la rama de eventos.
  tipo_evento text not null,
  fecha timestamptz not null,
  detalle text not null,
  fecha_baja timestamptz,
  fecha_modificacion timestamptz not null,
  rev integer not null default 1 check (rev >= 1),
  cursor_descarga bigint not null
);
create index on public.evento (id_perfil_finca);
create index on public.evento (id_animal);

create table public.registro (
  id_registro uuid primary key,
  id_animal uuid not null references public.animal,
  id_usuario uuid not null references public.usuario,
  id_perfil_finca uuid not null references public.perfil_finca,
  -- Nulo en un pesaje manual. Único: un pesaje del ESP32 se guarda una sola vez (RN-09, RN-21).
  id_lectura text unique,
  peso real not null check (peso > 0 and peso <= 1500),
  fecha_medicion timestamptz not null,
  fecha_baja timestamptz,
  fecha_modificacion timestamptz not null,
  rev integer not null default 1 check (rev >= 1),
  cursor_descarga bigint not null
);
create index on public.registro (id_perfil_finca);
create index on public.registro (id_animal);

create table public.chapeta (
  id_chapeta uuid primary key,
  -- Normalizado (RN-02): mayúsculas, sin espacios, `:` ni `-`.
  codigo text not null check (codigo <> '' and codigo = upper(codigo) and codigo !~ '[[:space:]:-]'),
  id_animal uuid not null references public.animal,
  id_perfil_finca uuid not null references public.perfil_finca,
  fecha_asociacion timestamptz not null,
  fecha_desasociacion timestamptz,
  fecha_modificacion timestamptz not null,
  cursor_descarga bigint not null
);
create index on public.chapeta (id_perfil_finca);
create index on public.chapeta (id_animal);
-- RN-01: un código tiene como máximo una Chapeta activa en cada finca.
create unique index chapeta_activa_por_codigo on public.chapeta (id_perfil_finca, codigo)
  where fecha_desasociacion is null;

create table public.pesaje_pendiente (
  id_pesaje_pendiente uuid primary key,
  id_lectura text not null unique,
  codigo text not null,
  peso real not null check (peso > 0 and peso <= 1500),
  fecha_medicion timestamptz not null,
  id_usuario uuid not null references public.usuario,
  id_perfil_finca uuid not null references public.perfil_finca,
  motivo text not null check (motivo in ('CODIGO_SIN_CHAPETA_ACTIVA', 'ANIMAL_DADO_DE_BAJA')),
  resolucion text check (resolucion in ('ASIGNADO', 'DESCARTADO')),
  id_usuario_resolucion uuid references public.usuario,
  fecha_resolucion timestamptz,
  fecha_modificacion timestamptz not null,
  cursor_descarga bigint not null,
  -- Las tres son nulas mientras está sin resolver y se llenan juntas al conciliar.
  constraint conciliacion_completa check (
    (resolucion is null) = (id_usuario_resolucion is null) and (resolucion is null) = (fecha_resolucion is null)
  )
);
create index on public.pesaje_pendiente (id_perfil_finca);

-- Historial completo de las filas con revisiones (RN-16). Lo escribe `subir_cambios`.
create table public.revision (
  id_version uuid primary key,
  entidad text not null check (entidad in ('animal', 'evento', 'registro')),
  id_fila uuid not null,
  id_perfil_finca uuid not null references public.perfil_finca,
  rev integer not null check (rev >= 1),
  contenido jsonb not null,
  id_usuario uuid not null default auth.uid() references public.usuario,
  fecha timestamptz not null default now(),
  unique (entidad, id_fila, rev)
);

-- Ediciones que llegaron partiendo de una revisión que ya no era la vigente (ADR-0004). Se conservan
-- después de resolverse; `resuelto_por` es el `id_version` de la edición que las resolvió.
create table public.sibling (
  id_version uuid primary key,
  entidad text not null check (entidad in ('animal', 'evento', 'registro')),
  id_fila uuid not null,
  id_perfil_finca uuid not null references public.perfil_finca,
  rev_base integer not null check (rev_base >= 1),
  contenido jsonb not null,
  id_usuario uuid not null default auth.uid() references public.usuario,
  fecha_recepcion timestamptz not null default now(),
  resuelto_por uuid,
  fecha_resolucion timestamptz,
  cursor_descarga bigint not null,
  constraint resolucion_completa check ((resuelto_por is null) = (fecha_resolucion is null))
);
create index on public.sibling (id_perfil_finca);
create index on public.sibling (entidad, id_fila) where resuelto_por is null;

-- Toda escritura en una tabla que se descarga toma un cursor nuevo; el cliente no puede fijarlo.
create trigger asignar_cursor_descarga before insert or update on public.perfil_finca
  for each row execute function privado.asignar_cursor_descarga();
create trigger asignar_cursor_descarga before insert or update on public.usuario
  for each row execute function privado.asignar_cursor_descarga();
create trigger asignar_cursor_descarga before insert or update on public.animal
  for each row execute function privado.asignar_cursor_descarga();
create trigger asignar_cursor_descarga before insert or update on public.evento
  for each row execute function privado.asignar_cursor_descarga();
create trigger asignar_cursor_descarga before insert or update on public.registro
  for each row execute function privado.asignar_cursor_descarga();
create trigger asignar_cursor_descarga before insert or update on public.chapeta
  for each row execute function privado.asignar_cursor_descarga();
create trigger asignar_cursor_descarga before insert or update on public.pesaje_pendiente
  for each row execute function privado.asignar_cursor_descarga();
create trigger asignar_cursor_descarga before insert or update on public.sibling
  for each row execute function privado.asignar_cursor_descarga();
create index on public.perfil_finca (cursor_descarga);
create index on public.usuario (cursor_descarga);
create index on public.animal (cursor_descarga);
create index on public.evento (cursor_descarga);
create index on public.registro (cursor_descarga);
create index on public.chapeta (cursor_descarga);
create index on public.pesaje_pendiente (cursor_descarga);
create index on public.sibling (cursor_descarga);
