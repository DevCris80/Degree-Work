-- Quitar a un Operario de la finca (CU-05, RF-37, LIM-06): el Ganadero revoca su acceso y sus registros anteriores
-- se conservan a su nombre. La fila de `usuario` no se borra (Evento, Registro y Pesaje pendiente la referencian y
-- nadie borra, RNF-04): se marca con `fecha_quitado`, y desde ese momento el Usuario ya no pertenece a ninguna finca
-- para las políticas RLS.

alter table public.usuario add column fecha_quitado timestamptz;

-- Un Usuario quitado no tiene finca ni rol: todas las políticas por finca y rol dejan de dejarlo pasar, así que
-- no lee ni escribe nada, ni genera códigos de invitación. Las mismas funciones, con una condición más.
create or replace function privado.mi_finca() returns uuid
language sql stable security definer set search_path = '' as $$
  select id_perfil_finca from public.usuario where id_usuario = auth.uid() and fecha_quitado is null
$$;

create or replace function privado.mi_rol() returns text
language sql stable security definer set search_path = '' as $$
  select rol from public.usuario where id_usuario = auth.uid() and fecha_quitado is null
$$;

-- Solo un Ganadero quita, y solo a un Operario que sigue en su finca. Cambia la fecha de modificación y, con
-- ella, el cursor de descarga (trigger asignar_cursor_descarga), así que los celulares lo bajan con la sincronización.
create function public.quitar_operario(id_operario uuid) returns void
language plpgsql security definer set search_path = '' as $$
declare
  finca uuid := privado.mi_finca();
begin
  if auth.uid() is null or finca is null then
    raise exception 'Sin sesión de una finca no se quita a nadie' using errcode = '42501';
  end if;
  if privado.mi_rol() is distinct from 'Ganadero' then
    raise exception 'Solo un Ganadero quita a un Operario' using errcode = '42501';
  end if;

  update public.usuario
    set fecha_quitado = now(), fecha_modificacion = now()
    where id_usuario = id_operario and id_perfil_finca = finca and rol = 'Operario' and fecha_quitado is null;
  if not found then
    raise exception 'No hay un Operario de la finca con el id %', id_operario using errcode = '22023';
  end if;
end;
$$;

revoke execute on function public.quitar_operario(uuid) from public, anon;
grant execute on function public.quitar_operario(uuid) to authenticated;

-- Si la cuenta de la sesión sigue en una finca. El celular lo pregunta al empezar a sincronizar: un Operario quitado
-- no sube nada y cierra su sesión (LIM-06). Responde aunque la cuenta ya no pase por RLS.
create function public.sigo_en_la_finca() returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (select 1 from public.usuario where id_usuario = auth.uid() and fecha_quitado is null)
$$;

revoke execute on function public.sigo_en_la_finca() from public, anon;
grant execute on function public.sigo_en_la_finca() to authenticated;
