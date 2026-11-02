-- Arregla el canje de un código de invitación (#151). `privado.canjear_invitacion` (20261029120000_invitaciones.sql)
-- llamaba `invitacion` a su variable, igual que a la tabla, y el `update` final fallaba con 42702 ("column reference
-- is ambiguous"): el alta de un Operario con código se revertía siempre. La misma función, con la variable renombrada.
-- Conserva sus permisos: `create or replace` no los cambia, y solo la llama el trigger de `auth.users`.

create or replace function privado.canjear_invitacion(persona uuid, nombre_usuario text, codigo_invitacion text, version_autorizacion text)
returns void
language plpgsql set search_path = '' as $$
declare
  fila public.invitacion;
begin
  if not exists (select 1 from privado.texto_autorizacion t where t.version = version_autorizacion) then
    raise exception 'Sin aceptar un texto de autorización vigente no se crea la cuenta' using errcode = '22023';
  end if;
  if coalesce(btrim(nombre_usuario), '') = '' then
    raise exception 'Falta el nombre de la persona' using errcode = '22023';
  end if;

  -- `for update`: dos altas con el mismo código a la vez se turnan, y la segunda ya lo encuentra usado.
  select * into fila from public.invitacion i
    where i.codigo = upper(btrim(codigo_invitacion)) and i.id_usuario_canje is null and i.fecha_vencimiento > now()
    for update;
  if not found then
    raise exception 'El código de invitación no existe, ya se usó o venció' using errcode = '22023';
  end if;

  insert into public.usuario (id_usuario, id_perfil_finca, nombre, rol, fecha_modificacion)
    values (persona, fila.id_perfil_finca, btrim(nombre_usuario), 'Operario', now());
  insert into public.aceptacion_datos (id_usuario, version_texto) values (persona, version_autorizacion);
  update public.invitacion i set id_usuario_canje = persona, fecha_canje = now()
    where i.id_invitacion = fila.id_invitacion;
end;
$$;
