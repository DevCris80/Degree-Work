-- Reglas que una política RLS no puede expresar porque dependen del valor anterior de la fila.
--
-- - Estados definitivos (ADR-0004 regla 4, RN-11): una baja, una Chapeta liberada, un Pesaje pendiente
--   conciliado y un sibling resuelto no se deshacen ni se cambian. Una Chapeta y un Pesaje pendiente solo
--   cambian de estado; el resto de la fila no se edita. Error 55000 (object_not_in_prerequisite_state).
-- - Permisos del Operario (matriz de la SRS, 2.1): no da de baja un Animal, no libera una Chapeta ni
--   descarta un Pesaje pendiente, tampoco al subir una fila que ya llega en ese estado. Error 42501, el
--   mismo de una política RLS.
--
-- Corren después de `a_finca` y `b_referencias` (los triggers de una tabla van en orden alfabético).

create function privado.reglas_de_animal() returns trigger
language plpgsql set search_path = '' as $$
begin
  if tg_op = 'UPDATE' then
    if new.id_perfil_finca is distinct from old.id_perfil_finca then
      raise exception 'Un Animal no cambia de finca' using errcode = '55000';
    end if;
    if old.fecha_baja is not null and new.fecha_baja is distinct from old.fecha_baja then
      raise exception 'La baja de un Animal es definitiva' using errcode = '55000';
    end if;
  end if;
  if new.fecha_baja is not null and (tg_op = 'INSERT' or old.fecha_baja is null)
     and privado.mi_rol() = 'Operario' then
    raise exception 'Un Operario no puede dar de baja un Animal' using errcode = '42501';
  end if;
  return new;
end;
$$;

-- Evento y Registro: se editan, pero no cambian de Animal ni de autor, un Registro conserva su
-- `id_lectura`, y la baja es definitiva. Dar de baja un Evento o un pesaje sigue permitido al Operario
-- mientras la matriz lo marque como propuesta.
create function privado.reglas_de_evento_o_registro() returns trigger
language plpgsql set search_path = '' as $$
declare
  antes jsonb := to_jsonb(old);
  despues jsonb := to_jsonb(new);
begin
  if tg_op = 'UPDATE' then
    if jsonb_build_array(despues -> 'id_animal', despues -> 'id_usuario', despues -> 'id_lectura')
       is distinct from jsonb_build_array(antes -> 'id_animal', antes -> 'id_usuario', antes -> 'id_lectura') then
      raise exception 'Un % no cambia de Animal, de autor ni de id_lectura', tg_table_name using errcode = '55000';
    end if;
    if old.fecha_baja is not null and new.fecha_baja is distinct from old.fecha_baja then
      raise exception 'La baja de un % es definitiva', tg_table_name using errcode = '55000';
    end if;
  end if;
  return new;
end;
$$;

create function privado.reglas_de_chapeta() returns trigger
language plpgsql set search_path = '' as $$
declare
  cambian constant text[] := array['fecha_desasociacion', 'fecha_modificacion', 'cursor_descarga'];
begin
  if tg_op = 'UPDATE' then
    if (to_jsonb(new) - cambian) is distinct from (to_jsonb(old) - cambian) then
      raise exception 'De una Chapeta solo cambia su fecha de desasociación' using errcode = '55000';
    end if;
    if old.fecha_desasociacion is not null and new.fecha_desasociacion is distinct from old.fecha_desasociacion then
      raise exception 'Liberar una Chapeta es definitivo' using errcode = '55000';
    end if;
  end if;
  if new.fecha_desasociacion is not null and (tg_op = 'INSERT' or old.fecha_desasociacion is null)
     and privado.mi_rol() = 'Operario' then
    raise exception 'Un Operario no puede liberar una Chapeta' using errcode = '42501';
  end if;
  return new;
end;
$$;

create function privado.reglas_de_pesaje_pendiente() returns trigger
language plpgsql set search_path = '' as $$
declare
  cambian constant text[] :=
    array['resolucion', 'id_usuario_resolucion', 'fecha_resolucion', 'fecha_modificacion', 'cursor_descarga'];
begin
  if tg_op = 'UPDATE' then
    if (to_jsonb(new) - cambian) is distinct from (to_jsonb(old) - cambian) then
      raise exception 'De un Pesaje pendiente solo cambia su conciliación' using errcode = '55000';
    end if;
    if old.resolucion is not null
       and (new.resolucion, new.id_usuario_resolucion, new.fecha_resolucion)
           is distinct from (old.resolucion, old.id_usuario_resolucion, old.fecha_resolucion) then
      raise exception 'Conciliar un Pesaje pendiente es definitivo' using errcode = '55000';
    end if;
  end if;
  if new.resolucion = 'DESCARTADO' and (tg_op = 'INSERT' or old.resolucion is null)
     and privado.mi_rol() = 'Operario' then
    raise exception 'Un Operario no puede descartar un Pesaje pendiente' using errcode = '42501';
  end if;
  return new;
end;
$$;

create function privado.reglas_de_sibling() returns trigger
language plpgsql set search_path = '' as $$
declare
  cambian constant text[] := array['resuelto_por', 'fecha_resolucion', 'cursor_descarga'];
begin
  if (to_jsonb(new) - cambian) is distinct from (to_jsonb(old) - cambian) then
    raise exception 'De un sibling solo cambia su resolución' using errcode = '55000';
  end if;
  if old.resuelto_por is not null
     and (new.resuelto_por, new.fecha_resolucion) is distinct from (old.resuelto_por, old.fecha_resolucion) then
    raise exception 'Un sibling resuelto no cambia' using errcode = '55000';
  end if;
  return new;
end;
$$;

create trigger c_reglas before insert or update on public.animal
  for each row execute function privado.reglas_de_animal();
create trigger c_reglas before update on public.evento
  for each row execute function privado.reglas_de_evento_o_registro();
create trigger c_reglas before update on public.registro
  for each row execute function privado.reglas_de_evento_o_registro();
create trigger c_reglas before insert or update on public.chapeta
  for each row execute function privado.reglas_de_chapeta();
create trigger c_reglas before insert or update on public.pesaje_pendiente
  for each row execute function privado.reglas_de_pesaje_pendiente();
create trigger c_reglas before update on public.sibling
  for each row execute function privado.reglas_de_sibling();
