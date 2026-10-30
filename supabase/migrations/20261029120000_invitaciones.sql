-- Unirse a una finca con un código de invitación (CU-04, RF-05, RN-18).
--
-- El Ganadero llama `generar_invitacion` y recibe un código de un solo uso que vence a los 7 días. La persona que
-- lo recibe crea su cuenta con ese código entre los metadatos del registro (`codigo_invitacion`); el trigger sobre
-- `auth.users` llama `privado.canjear_invitacion` en la misma transacción del alta, igual que `crear_finca`: la
-- identidad, su Usuario Operario, la aceptación del texto y el canje del código se crean juntos, o no se crea nada.
-- Un código usado, vencido o inexistente rechaza el alta y no deja la identidad.
--
-- Los metadatos no deciden el rol ni la finca: la finca sale del código y el rol siempre es Operario.

-- Solo en el servidor: ningún celular la lee ni la escribe por la API.
create table public.invitacion (
  id_invitacion uuid primary key default gen_random_uuid(),
  id_perfil_finca uuid not null references public.perfil_finca,
  codigo text not null unique check (codigo = upper(codigo)),
  id_usuario_creador uuid not null references auth.users (id),
  fecha_creacion timestamptz not null default now(),
  fecha_vencimiento timestamptz not null,
  id_usuario_canje uuid references auth.users (id),
  fecha_canje timestamptz,
  check ((id_usuario_canje is null) = (fecha_canje is null))
);
create index on public.invitacion (id_perfil_finca);

revoke all on public.invitacion from anon, authenticated;
alter table public.invitacion enable row level security;

-- Sin O/0 ni I/1/L, que se confunden al dictar o leer el código.
create function privado.codigo_de_invitacion() returns text
language plpgsql set search_path = '' as $$
declare
  alfabeto constant text := 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
  codigo text := '';
begin
  for i in 1..8 loop
    codigo := codigo || substr(alfabeto, 1 + floor(random() * length(alfabeto))::int, 1);
  end loop;
  return codigo;
end;
$$;

create function public.generar_invitacion() returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  finca uuid := privado.mi_finca();
  nueva public.invitacion;
begin
  if auth.uid() is null or finca is null then
    raise exception 'Sin sesión de una finca no se generan códigos' using errcode = '42501';
  end if;
  if privado.mi_rol() is distinct from 'Ganadero' then
    raise exception 'Solo un Ganadero genera códigos de invitación' using errcode = '42501';
  end if;

  -- 31^8 combinaciones: una colisión es casi imposible, pero la unicidad la hace cumplir la tabla.
  loop
    begin
      insert into public.invitacion (id_perfil_finca, codigo, id_usuario_creador, fecha_vencimiento)
        values (finca, privado.codigo_de_invitacion(), auth.uid(), now() + interval '7 days')
        returning * into nueva;
      exit;
    exception when unique_violation then
      null;
    end;
  end loop;

  return jsonb_build_object('codigo', nueva.codigo, 'fecha_vencimiento', nueva.fecha_vencimiento);
end;
$$;

revoke execute on function public.generar_invitacion() from public, anon;
grant execute on function public.generar_invitacion() to authenticated;

create function privado.canjear_invitacion(persona uuid, nombre_usuario text, codigo_invitacion text, version_autorizacion text)
returns void
language plpgsql set search_path = '' as $$
declare
  invitacion public.invitacion;
begin
  if not exists (select 1 from privado.texto_autorizacion t where t.version = version_autorizacion) then
    raise exception 'Sin aceptar un texto de autorización vigente no se crea la cuenta' using errcode = '22023';
  end if;
  if coalesce(btrim(nombre_usuario), '') = '' then
    raise exception 'Falta el nombre de la persona' using errcode = '22023';
  end if;

  -- `for update`: dos altas con el mismo código a la vez se turnan, y la segunda ya lo encuentra usado.
  select * into invitacion from public.invitacion i
    where i.codigo = upper(btrim(codigo_invitacion)) and i.id_usuario_canje is null and i.fecha_vencimiento > now()
    for update;
  if not found then
    raise exception 'El código de invitación no existe, ya se usó o venció' using errcode = '22023';
  end if;

  insert into public.usuario (id_usuario, id_perfil_finca, nombre, rol, fecha_modificacion)
    values (persona, invitacion.id_perfil_finca, btrim(nombre_usuario), 'Operario', now());
  insert into public.aceptacion_datos (id_usuario, version_texto) values (persona, version_autorizacion);
  update public.invitacion set id_usuario_canje = persona, fecha_canje = now()
    where id_invitacion = invitacion.id_invitacion;
end;
$$;

revoke execute on function privado.canjear_invitacion(uuid, text, text, text) from public, anon, authenticated;

-- Un alta con `nombre_finca` crea una finca; con `codigo_invitacion`, se une a una. Con los dos no se sabe qué quiere.
create or replace function privado.crear_finca_al_registrarse() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  if new.raw_user_meta_data ? 'nombre_finca' and new.raw_user_meta_data ? 'codigo_invitacion' then
    raise exception 'Un alta crea una finca o se une a una con un código, no las dos' using errcode = '22023';
  elsif new.raw_user_meta_data ? 'nombre_finca' then
    perform privado.crear_finca(
      new.id,
      new.raw_user_meta_data ->> 'nombre',
      new.raw_user_meta_data ->> 'nombre_finca',
      new.raw_user_meta_data ->> 'version_autorizacion'
    );
  elsif new.raw_user_meta_data ? 'codigo_invitacion' then
    perform privado.canjear_invitacion(
      new.id,
      new.raw_user_meta_data ->> 'nombre',
      new.raw_user_meta_data ->> 'codigo_invitacion',
      new.raw_user_meta_data ->> 'version_autorizacion'
    );
  end if;
  return new;
end;
$$;
