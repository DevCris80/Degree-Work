-- Subida de los cambios locales (CU-20, RF-32, RF-33, ADR-0004, ADR-0005).
--
-- `subir_cambios(lote)` recibe un arreglo de cambios y devuelve un resultado por cambio, en el mismo orden.
-- Corre como el usuario (SECURITY INVOKER), así que las políticas RLS y los triggers de las tablas aplican.
--
-- Por lo mismo, `authenticated` tiene INSERT y UPDATE directos sobre las tablas. Para que RN-15 y RN-16 no
-- dependan de que el cliente pase por la función, `subir_cambios` marca su transacción mientras corre, y un
-- trigger rechaza (42501) toda escritura de `authenticated` o `anon` en Animal, Evento, Registro, revisiones y
-- siblings que no lleve la marca. PostgREST no deja que un cliente fije esa configuración en una petición directa.
--
-- Un cambio:   {"entidad": "animal", "id_version": uuid, "rev_base": int | null, "resuelve": [uuid] | null,
--               "fila": {las columnas de la tabla, en snake_case}}
-- Un resultado: {"entidad", "id_fila", "id_version", "resultado": "aceptada" | "sibling" | "rechazada",
--               "rev", "fila": la fila del servidor después del cambio, "codigo" y "motivo" si se rechaza}

create function privado.solo_por_subir_cambios() returns trigger
language plpgsql set search_path = '' as $$
begin
  if current_user in ('anon', 'authenticated')
     and current_setting('miganado.subir_cambios', true) is distinct from 'si' then
    raise exception 'La tabla % solo se escribe con subir_cambios', tg_table_name using errcode = '42501';
  end if;
  return new;
end;
$$;

-- Corre antes que los demás triggers de la tabla, que van en orden alfabético.
create trigger a0_solo_por_subir_cambios before insert or update on public.animal
  for each row execute function privado.solo_por_subir_cambios();
create trigger a0_solo_por_subir_cambios before insert or update on public.evento
  for each row execute function privado.solo_por_subir_cambios();
create trigger a0_solo_por_subir_cambios before insert or update on public.registro
  for each row execute function privado.solo_por_subir_cambios();
create trigger a0_solo_por_subir_cambios before insert or update on public.revision
  for each row execute function privado.solo_por_subir_cambios();
create trigger a0_solo_por_subir_cambios before insert or update on public.sibling
  for each row execute function privado.solo_por_subir_cambios();

-- Las columnas que una subida puede cambiar: todas menos la llave y `cursor_descarga`. `rev` la pone siempre
-- `subir_con_revisiones`, nunca el cliente.
create function privado.columnas_editables(tabla text) returns text[]
language sql stable set search_path = '' as $$
  select array_agg(a.attname::text order by a.attnum)
  from pg_catalog.pg_attribute a
  where a.attrelid = ('public.' || tabla)::regclass and a.attnum > 0 and not a.attisdropped
    and a.attname not in ('id_' || tabla, 'cursor_descarga')
$$;

-- La fila tal como queda en el servidor, sin el cursor, que solo le sirve a la descarga.
create function privado.leer_fila(tabla text, id uuid, bloquear boolean default false) returns jsonb
language plpgsql set search_path = '' as $$
declare
  fila jsonb;
begin
  execute format('select to_jsonb(t) - ''cursor_descarga'' from public.%I t where t.%I = $1 %s',
    tabla, 'id_' || tabla, case when bloquear then 'for update' end)
    into fila using id;
  return fila;
end;
$$;

create function privado.insertar_fila(tabla text, fila jsonb) returns jsonb
language plpgsql set search_path = '' as $$
declare
  nueva jsonb;
begin
  execute format(
    'insert into public.%I as t select * from jsonb_populate_record(null::public.%I, $1) '
      'returning to_jsonb(t) - ''cursor_descarga''',
    tabla, tabla)
    into nueva using fila;
  return nueva;
end;
$$;

-- Escribe sobre la fila existente las columnas que trae `cambios`; las que no trae quedan como estaban.
create function privado.actualizar_fila(tabla text, id uuid, cambios jsonb) returns jsonb
language plpgsql set search_path = '' as $$
declare
  columnas constant text[] := privado.columnas_editables(tabla);
  nueva jsonb;
begin
  execute format(
    'update public.%I t set (%s) = (select %s from jsonb_populate_record(t, $1) n) where t.%I = $2 '
      'returning to_jsonb(t) - ''cursor_descarga''',
    tabla,
    (select string_agg(format('%I', c), ', ') from unnest(columnas) c),
    (select string_agg(format('n.%I', c), ', ') from unnest(columnas) c),
    'id_' || tabla)
    into nueva using cambios, id;
  return nueva;
end;
$$;

-- Si `despues` cambia algo respecto de `antes`, sin contar las columnas `no_cuentan`. Compara solo las columnas
-- que trae `despues`, leídas con el tipo de la tabla para que una misma fecha escrita de dos formas no cuente.
create function privado.cambia(tabla text, antes jsonb, despues jsonb, no_cuentan text[]) returns boolean
language plpgsql set search_path = '' as $$
declare
  a jsonb;
  d jsonb;
begin
  execute format(
    'select to_jsonb(jsonb_populate_record(null::public.%I, $1)), to_jsonb(jsonb_populate_record(null::public.%I, $2))',
    tabla, tabla)
    into a, d using antes, despues;
  return exists (
    select 1 from jsonb_object_keys(despues) k where k <> all (no_cuentan) and a -> k is distinct from d -> k
  );
end;
$$;

create function privado.guardar_revision(id_version uuid, tabla text, nueva jsonb) returns jsonb
language sql set search_path = '' as $$
  insert into public.revision (id_version, entidad, id_fila, id_perfil_finca, rev, contenido)
    values (id_version, tabla, (nueva ->> ('id_' || tabla))::uuid, (nueva ->> 'id_perfil_finca')::uuid,
      (nueva ->> 'rev')::integer, nueva);
  select jsonb_build_object('resultado', 'aceptada', 'rev', (nueva ->> 'rev')::integer, 'fila', nueva);
$$;

-- Animal, Evento y Registro (ADR-0004).
create function privado.subir_con_revisiones(cambio jsonb) returns jsonb
language plpgsql set search_path = '' as $$
declare
  tabla constant text := cambio ->> 'entidad';
  version constant uuid := (cambio ->> 'id_version')::uuid;
  fila constant jsonb := cambio -> 'fila';
  id constant uuid := (fila ->> ('id_' || tabla))::uuid;
  -- Sin `rev_base`, el celular nunca recibió la respuesta de su inserción. Si la fila ya está en el servidor,
  -- solo pudo llegar por esa inserción, porque el UUID lo generó ese celular: la edición parte de la revisión 1.
  rev_base constant integer := coalesce((cambio ->> 'rev_base')::integer, 1);
  vigente jsonb;
  rev_vigente integer;
  nueva jsonb;
begin
  -- RN-23: un id_version ya procesado recibe la misma respuesta, con la fila como está ahora. El sibling va
  -- primero, porque una baja que además cambió otros campos dejó las dos cosas con el mismo id_version.
  if exists (select 1 from public.sibling s where s.id_version = version) then
    vigente := privado.leer_fila(tabla, id);
    return jsonb_build_object('resultado', 'sibling', 'rev', (vigente ->> 'rev')::integer, 'fila', vigente);
  end if;
  if exists (select 1 from public.revision r where r.id_version = version) then
    return jsonb_build_object('resultado', 'aceptada',
      'rev', (select r.rev from public.revision r where r.id_version = version),
      'fila', privado.leer_fila(tabla, id));
  end if;

  vigente := privado.leer_fila(tabla, id, bloquear => true);
  if vigente is null then
    return privado.guardar_revision(version, tabla, privado.insertar_fila(tabla, fila || '{"rev": 1}'));
  end if;
  rev_vigente := (vigente ->> 'rev')::integer;
  if rev_base = rev_vigente then
    nueva := privado.guardar_revision(version, tabla,
      privado.actualizar_fila(tabla, id, fila || jsonb_build_object('rev', rev_vigente + 1)));
    -- RN-17: una resolución cierra los siblings que nombra, si siguen abiertos. Uno que llegó después no lo nombra.
    update public.sibling s set resuelto_por = version, fecha_resolucion = now()
      where s.entidad = tabla and s.id_fila = id and s.resuelto_por is null and s.id_version in (
        select r::uuid from jsonb_array_elements_text(
          case when jsonb_typeof(cambio -> 'resuelve') = 'array' then cambio -> 'resuelve' else '[]' end) r);
    return nueva;
  end if;

  -- RN-22: la baja nunca es sibling; se aplica sobre la vigente. Lo demás que cambió esa edición, si cambió
  -- algo respecto de la revisión de la que partió, queda como sibling.
  if vigente ->> 'fecha_baja' is null and fila ->> 'fecha_baja' is not null then
    nueva := privado.guardar_revision(version, tabla, privado.actualizar_fila(tabla, id, jsonb_build_object(
      'fecha_baja', fila -> 'fecha_baja', 'fecha_modificacion', fila -> 'fecha_modificacion', 'rev', rev_vigente + 1)));
    if not privado.cambia(tabla,
      (select r.contenido from public.revision r where r.entidad = tabla and r.id_fila = id and r.rev = rev_base),
      fila, array['fecha_baja', 'fecha_modificacion', 'id_perfil_finca']) then
      return nueva;
    end if;
    vigente := nueva -> 'fila';
  end if;
  insert into public.sibling (id_version, entidad, id_fila, id_perfil_finca, rev_base, contenido)
    values (version, tabla, id, (vigente ->> 'id_perfil_finca')::uuid, rev_base, fila);
  return jsonb_build_object('resultado', 'sibling', 'rev', (vigente ->> 'rev')::integer, 'fila', vigente);
end;
$$;

-- Chapeta y Pesaje pendiente (ADR-0004, regla 6): se insertan por UUID y después solo cambian de estado. Los
-- triggers de la tabla rechazan cualquier otro cambio. Reenviar el mismo estado no reescribe la fila.
create function privado.subir_sin_revisiones(cambio jsonb) returns jsonb
language plpgsql set search_path = '' as $$
declare
  tabla constant text := cambio ->> 'entidad';
  fila constant jsonb := cambio -> 'fila';
  id constant uuid := (fila ->> ('id_' || tabla))::uuid;
  existente jsonb;
begin
  existente := privado.leer_fila(tabla, id, bloquear => true);
  if existente is null then
    existente := privado.insertar_fila(tabla, fila);
  elsif privado.cambia(tabla, existente, fila, array['fecha_modificacion', 'id_perfil_finca']) then
    existente := privado.actualizar_fila(tabla, id, fila);
  end if;
  return jsonb_build_object('resultado', 'aceptada', 'fila', existente);
end;
$$;

-- PerfilFinca (ADR-0004, regla 6): la crea el servidor y la edita solo su Ganadero, sin revisiones. Cambia solo
-- las columnas que trae el cambio, así que un celular que no conoce la ubicación no la borra.
create function privado.subir_perfil_finca(cambio jsonb) returns jsonb
language plpgsql set search_path = '' as $$
declare
  fila constant jsonb := cambio -> 'fila';
  id constant uuid := (fila ->> 'id_perfil_finca')::uuid;
  existente jsonb;
begin
  -- La política de UPDATE deja ver para editar solo la finca propia, y solo al Ganadero.
  existente := privado.leer_fila('perfil_finca', id, bloquear => true);
  if existente is null then
    raise exception 'Solo el Ganadero edita los datos de su finca' using errcode = '42501';
  end if;
  if privado.cambia('perfil_finca', existente, fila, array['fecha_modificacion']) then
    existente := privado.actualizar_fila('perfil_finca', id, fila);
  end if;
  return jsonb_build_object('resultado', 'aceptada', 'fila', existente);
end;
$$;

-- Un cambio del lote. Un error de unicidad, de permisos, de referencia, de estado o de datos (los códigos de
-- supabase/README.md) rechaza ese cambio sin abortar el lote: el bloque con EXCEPTION deshace solo lo suyo. El
-- rechazo lleva la fila como está en el servidor, o nula si el servidor no la tiene.
create function privado.subir_cambio(cambio jsonb) returns jsonb
language plpgsql set search_path = '' as $$
declare
  tabla constant text := cambio ->> 'entidad';
  codigo text;
  motivo text;
  en_servidor jsonb;
begin
  if tabla is null or tabla not in ('perfil_finca', 'animal', 'evento', 'registro', 'chapeta', 'pesaje_pendiente') then
    return jsonb_build_object(
      'resultado', 'rechazada', 'codigo', '22023', 'motivo', 'Entidad desconocida', 'fila', null);
  end if;
  begin
    if tabla in ('animal', 'evento', 'registro') then
      return privado.subir_con_revisiones(cambio);
    elsif tabla = 'perfil_finca' then
      return privado.subir_perfil_finca(cambio);
    else
      return privado.subir_sin_revisiones(cambio);
    end if;
  exception when integrity_constraint_violation or insufficient_privilege or object_not_in_prerequisite_state
    or data_exception then
    get stacked diagnostics codigo = returned_sqlstate, motivo = message_text;
  end;
  begin
    en_servidor := privado.leer_fila(tabla, (cambio -> 'fila' ->> ('id_' || tabla))::uuid);
  exception when data_exception then
    en_servidor := null;
  end;
  return jsonb_build_object('resultado', 'rechazada', 'codigo', codigo, 'motivo', motivo, 'fila', en_servidor);
end;
$$;

create function public.subir_cambios(lote jsonb) returns jsonb
language plpgsql security invoker set search_path = '' as $$
declare
  cambio jsonb;
  resultados jsonb := '[]';
begin
  perform set_config('miganado.subir_cambios', 'si', true);
  -- Un Pesaje pendiente asignado sin su Registro se rechaza en su propio cambio, no al confirmar el lote entero.
  -- El celular sube los Registros antes que los Pesajes pendientes.
  set constraints all immediate;
  for cambio in select * from jsonb_array_elements(lote) loop
    resultados := resultados || jsonb_build_array(jsonb_build_object(
      'entidad', cambio ->> 'entidad',
      'id_fila', cambio -> 'fila' ->> ('id_' || (cambio ->> 'entidad')),
      'id_version', cambio -> 'id_version'
    ) || privado.subir_cambio(cambio));
  end loop;
  perform set_config('miganado.subir_cambios', '', true);
  return resultados;
end;
$$;

-- Solo una sesión iniciada sube cambios. Las funciones de `privado` no las expone PostgREST.
revoke execute on function public.subir_cambios(jsonb) from public, anon;
grant execute on function public.subir_cambios(jsonb) to authenticated;
