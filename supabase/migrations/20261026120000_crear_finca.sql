-- Crear cuenta (CU-01, RF-01, RF-04, RN-20, RNF-13).
--
-- El celular crea primero la identidad en Supabase Auth y después llama `crear_finca` con el JWT de esa persona.
-- La función crea, en una sola transacción, la finca, su primer Usuario con rol Ganadero y el registro de que la
-- persona aceptó el texto de autorización de la Ley 1581, con su versión y su fecha. `perfil_finca` y `usuario`
-- no conceden INSERT a `authenticated`, así que la función es SECURITY DEFINER y solo escribe a nombre de
-- `auth.uid()`.
--
-- Nadie tiene dos fincas: si la persona ya es Usuario de una (un reintento después de perder la respuesta, o un
-- Operario), devuelve la suya sin crear nada.
--
-- Respuesta: {"perfil_finca": la fila de la finca, "usuario": la fila del Usuario}, sin `cursor_descarga`.

-- Solo en el servidor (diagrama de relaciones): ningún celular la descarga.
create table public.aceptacion_datos (
  id_aceptacion uuid primary key default gen_random_uuid(),
  id_usuario uuid not null references auth.users (id),
  version_texto text not null check (btrim(version_texto) <> ''),
  fecha_aceptacion timestamptz not null default now()
);
create index on public.aceptacion_datos (id_usuario);

revoke all on public.aceptacion_datos from anon, authenticated;
alter table public.aceptacion_datos enable row level security;

create function public.crear_finca(nombre_usuario text, nombre_finca text, version_autorizacion text) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  persona uuid := auth.uid();
  finca public.perfil_finca;
  nuevo public.usuario;
begin
  if persona is null then
    raise exception 'Crear una finca necesita una sesión' using errcode = '42501';
  end if;

  select * into nuevo from public.usuario where id_usuario = persona;
  if found then
    select * into finca from public.perfil_finca where id_perfil_finca = nuevo.id_perfil_finca;
    return jsonb_build_object(
      'perfil_finca', to_jsonb(finca) - 'cursor_descarga',
      'usuario', to_jsonb(nuevo) - 'cursor_descarga'
    );
  end if;

  if coalesce(btrim(version_autorizacion), '') = '' then
    raise exception 'Sin aceptar el texto de autorización no se crea la cuenta' using errcode = '22023';
  end if;
  if coalesce(btrim(nombre_usuario), '') = '' or coalesce(btrim(nombre_finca), '') = '' then
    raise exception 'Faltan el nombre de la persona o el de la finca' using errcode = '22023';
  end if;

  insert into public.perfil_finca (id_perfil_finca, nombre_finca, fecha_modificacion)
    values (gen_random_uuid(), btrim(nombre_finca), now())
    returning * into finca;
  insert into public.usuario (id_usuario, id_perfil_finca, nombre, rol, fecha_modificacion)
    values (persona, finca.id_perfil_finca, btrim(nombre_usuario), 'Ganadero', now())
    returning * into nuevo;
  insert into public.aceptacion_datos (id_usuario, version_texto) values (persona, btrim(version_autorizacion));

  return jsonb_build_object(
    'perfil_finca', to_jsonb(finca) - 'cursor_descarga',
    'usuario', to_jsonb(nuevo) - 'cursor_descarga'
  );
end;
$$;

revoke execute on function public.crear_finca(text, text, text) from public, anon;
grant execute on function public.crear_finca(text, text, text) to authenticated;
