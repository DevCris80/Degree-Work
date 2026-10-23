-- Matriz de la SRS (2.1), confirmada el 2026-10-23: dar de baja un Evento o un Registro es solo del
-- Ganadero, igual que dar de baja un Animal. También se rechaza la fila que el Operario sube ya dada de baja.

create or replace function privado.reglas_de_evento_o_registro() returns trigger
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
  if new.fecha_baja is not null and (tg_op = 'INSERT' or old.fecha_baja is null)
     and privado.mi_rol() = 'Operario' then
    raise exception 'Un Operario no puede dar de baja un %', tg_table_name using errcode = '42501';
  end if;
  return new;
end;
$$;

-- Antes solo corría al modificar; ahora también al insertar.
drop trigger c_reglas on public.evento;
drop trigger c_reglas on public.registro;
create trigger c_reglas before insert or update on public.evento
  for each row execute function privado.reglas_de_evento_o_registro();
create trigger c_reglas before insert or update on public.registro
  for each row execute function privado.reglas_de_evento_o_registro();
