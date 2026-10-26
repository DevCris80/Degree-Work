-- Crear cuenta (CU-01, RF-01, RF-04, RN-20, RNF-13).
--
-- El celular registra la identidad en Supabase Auth con, como metadatos del registro, el nombre de la persona, el
-- de su finca y la versión del texto de autorización de la Ley 1581 que aceptó. Un trigger sobre `auth.users`
-- llama `privado.crear_finca` en la misma transacción del alta: la identidad, la finca, su primer Usuario con rol
-- Ganadero y la aceptación se crean juntas, o no se crea ninguna. Así, si el registro falla o se corta la señal
-- antes de que Supabase lo confirme, no queda una identidad sin finca.
--
-- Un alta sin `nombre_finca` en los metadatos (un Operario creado a mano, o las invitaciones de CU-04) no crea
-- finca. Los metadatos no deciden el rol ni la finca: el primer Usuario siempre es Ganadero de una finca nueva.

-- Los textos de autorización que la app puede mostrar. Cambiar el texto es una migración que agrega su versión.
create table privado.texto_autorizacion (
  version text primary key,
  fecha_publicacion timestamptz not null default now()
);
insert into privado.texto_autorizacion (version) values ('0.1-provisional');
revoke all on privado.texto_autorizacion from anon, authenticated;

-- Solo en el servidor (diagrama de relaciones): ningún celular la descarga.
create table public.aceptacion_datos (
  id_aceptacion uuid primary key default gen_random_uuid(),
  id_usuario uuid not null references auth.users (id),
  version_texto text not null references privado.texto_autorizacion (version),
  fecha_aceptacion timestamptz not null default now()
);
create index on public.aceptacion_datos (id_usuario);

revoke all on public.aceptacion_datos from anon, authenticated;
alter table public.aceptacion_datos enable row level security;

create function privado.crear_finca(persona uuid, nombre_usuario text, nombre_finca text, version_autorizacion text)
returns void
language plpgsql set search_path = '' as $$
declare
  finca uuid := gen_random_uuid();
begin
  if not exists (select 1 from privado.texto_autorizacion t where t.version = version_autorizacion) then
    raise exception 'Sin aceptar un texto de autorización vigente no se crea la cuenta' using errcode = '22023';
  end if;
  if coalesce(btrim(nombre_usuario), '') = '' or coalesce(btrim(nombre_finca), '') = '' then
    raise exception 'Faltan el nombre de la persona o el de la finca' using errcode = '22023';
  end if;

  insert into public.perfil_finca (id_perfil_finca, nombre_finca, fecha_modificacion)
    values (finca, btrim(nombre_finca), now());
  insert into public.usuario (id_usuario, id_perfil_finca, nombre, rol, fecha_modificacion)
    values (persona, finca, btrim(nombre_usuario), 'Ganadero', now());
  insert into public.aceptacion_datos (id_usuario, version_texto) values (persona, version_autorizacion);
end;
$$;

-- SECURITY DEFINER: corre como el dueño de la migración, no como `supabase_auth_admin`, que hace el alta y no
-- tiene permisos sobre las tablas de la finca.
create function privado.crear_finca_al_registrarse() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  if new.raw_user_meta_data ? 'nombre_finca' then
    perform privado.crear_finca(
      new.id,
      new.raw_user_meta_data ->> 'nombre',
      new.raw_user_meta_data ->> 'nombre_finca',
      new.raw_user_meta_data ->> 'version_autorizacion'
    );
  end if;
  return new;
end;
$$;

revoke execute on function privado.crear_finca(uuid, text, text, text) from public, anon, authenticated;
revoke execute on function privado.crear_finca_al_registrarse() from public, anon, authenticated;

create trigger crear_finca after insert on auth.users
  for each row execute function privado.crear_finca_al_registrarse();
