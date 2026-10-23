-- Cada cuenta solo ve y escribe las filas de su finca (RF-06, RNF-12). Las funciones de sincronización
-- corren como el usuario (SECURITY INVOKER, ADR-0005), así que estas políticas aplican también a ellas.
-- Nadie borra (RNF-04): no hay política de DELETE y el permiso está revocado.

-- La finca y el rol del usuario de la sesión. SECURITY DEFINER para leer `usuario` sin pasar por sus
-- propias políticas, que dependen de estas funciones.
create function privado.mi_finca() returns uuid
language sql stable security definer set search_path = '' as $$
  select id_perfil_finca from public.usuario where id_usuario = auth.uid()
$$;

create function privado.mi_rol() returns text
language sql stable security definer set search_path = '' as $$
  select rol from public.usuario where id_usuario = auth.uid()
$$;

-- Las tablas hijas toman la finca de su Animal o de su Usuario. Leer el padre pasa por RLS, así que una
-- fila que apunta a un Animal o a un Usuario de otra finca se rechaza como referencia inválida.
create function privado.tomar_finca_del_animal() returns trigger
language plpgsql set search_path = '' as $$
begin
  new.id_perfil_finca := (select a.id_perfil_finca from public.animal a where a.id_animal = new.id_animal);
  if new.id_perfil_finca is null then
    raise exception 'El animal % no existe en la finca', new.id_animal using errcode = '23503';
  end if;
  return new;
end;
$$;

create function privado.tomar_finca_del_usuario() returns trigger
language plpgsql set search_path = '' as $$
begin
  new.id_perfil_finca := (select u.id_perfil_finca from public.usuario u where u.id_usuario = new.id_usuario);
  if new.id_perfil_finca is null then
    raise exception 'El usuario % no existe en la finca', new.id_usuario using errcode = '23503';
  end if;
  return new;
end;
$$;

-- Los Usuarios y los Animales a los que apunta una fila son de su misma finca.
create function privado.exigir_misma_finca(tabla text, id uuid, finca uuid) returns void
language plpgsql set search_path = '' as $$
begin
  if id is null then
    return;
  end if;
  if tabla = 'usuario' and not exists (
    select 1 from public.usuario u where u.id_usuario = id and u.id_perfil_finca = finca
  ) or tabla = 'animal' and not exists (
    select 1 from public.animal a where a.id_animal = id and a.id_perfil_finca = finca
  ) then
    raise exception 'El % % no existe en la finca', tabla, id using errcode = '23503';
  end if;
end;
$$;

create function privado.referencias_de_animal() returns trigger
language plpgsql set search_path = '' as $$
begin
  perform privado.exigir_misma_finca('animal', new.id_padre, new.id_perfil_finca);
  perform privado.exigir_misma_finca('animal', new.id_madre, new.id_perfil_finca);
  return new;
end;
$$;

create function privado.referencias_de_autor() returns trigger
language plpgsql set search_path = '' as $$
begin
  perform privado.exigir_misma_finca('usuario', new.id_usuario, new.id_perfil_finca);
  return new;
end;
$$;

create function privado.referencias_de_pesaje_pendiente() returns trigger
language plpgsql set search_path = '' as $$
begin
  perform privado.exigir_misma_finca('usuario', new.id_usuario_resolucion, new.id_perfil_finca);
  return new;
end;
$$;

-- Los triggers de una misma tabla corren en orden alfabético: primero la finca, después las referencias.
create trigger a_finca before insert or update on public.evento
  for each row execute function privado.tomar_finca_del_animal();
create trigger a_finca before insert or update on public.registro
  for each row execute function privado.tomar_finca_del_animal();
create trigger a_finca before insert or update on public.chapeta
  for each row execute function privado.tomar_finca_del_animal();
create trigger a_finca before insert or update on public.pesaje_pendiente
  for each row execute function privado.tomar_finca_del_usuario();

create trigger b_referencias before insert or update on public.animal
  for each row execute function privado.referencias_de_animal();
create trigger b_referencias before insert or update on public.evento
  for each row execute function privado.referencias_de_autor();
create trigger b_referencias before insert or update on public.registro
  for each row execute function privado.referencias_de_autor();
create trigger b_referencias before insert or update on public.pesaje_pendiente
  for each row execute function privado.referencias_de_pesaje_pendiente();

-- Permisos de tabla. Supabase concede todo a anon y authenticated por defecto; aquí se deja solo lo
-- que cada tabla necesita, como segunda barrera detrás de las políticas.
revoke all on public.perfil_finca, public.usuario, public.animal, public.evento, public.registro,
  public.chapeta, public.pesaje_pendiente, public.revision, public.sibling from anon, authenticated;

-- `perfil_finca` y `usuario` los crean y cambian funciones del servidor (crear_finca, canjear_invitacion),
-- salvo los datos de la finca, que edita su Ganadero.
grant select, update on public.perfil_finca to authenticated;
grant select on public.usuario to authenticated;
grant select, insert, update on public.animal, public.evento, public.registro, public.chapeta,
  public.pesaje_pendiente, public.sibling to authenticated;
grant select, insert on public.revision to authenticated;

alter table public.perfil_finca enable row level security;
alter table public.usuario enable row level security;
alter table public.animal enable row level security;
alter table public.evento enable row level security;
alter table public.registro enable row level security;
alter table public.chapeta enable row level security;
alter table public.pesaje_pendiente enable row level security;
alter table public.revision enable row level security;
alter table public.sibling enable row level security;

create policy "ver su finca" on public.perfil_finca for select to authenticated
  using (id_perfil_finca = privado.mi_finca());
create policy "el Ganadero edita su finca" on public.perfil_finca for update to authenticated
  using (id_perfil_finca = privado.mi_finca() and privado.mi_rol() = 'Ganadero')
  with check (id_perfil_finca = privado.mi_finca());

create policy "ver los usuarios de su finca" on public.usuario for select to authenticated
  using (id_perfil_finca = privado.mi_finca());

create policy "ver su finca" on public.animal for select to authenticated
  using (id_perfil_finca = privado.mi_finca());
create policy "crear en su finca" on public.animal for insert to authenticated
  with check (id_perfil_finca = privado.mi_finca());
create policy "editar en su finca" on public.animal for update to authenticated
  using (id_perfil_finca = privado.mi_finca()) with check (id_perfil_finca = privado.mi_finca());

create policy "ver su finca" on public.evento for select to authenticated
  using (id_perfil_finca = privado.mi_finca());
create policy "crear en su finca" on public.evento for insert to authenticated
  with check (id_perfil_finca = privado.mi_finca());
create policy "editar en su finca" on public.evento for update to authenticated
  using (id_perfil_finca = privado.mi_finca()) with check (id_perfil_finca = privado.mi_finca());

create policy "ver su finca" on public.registro for select to authenticated
  using (id_perfil_finca = privado.mi_finca());
create policy "crear en su finca" on public.registro for insert to authenticated
  with check (id_perfil_finca = privado.mi_finca());
create policy "editar en su finca" on public.registro for update to authenticated
  using (id_perfil_finca = privado.mi_finca()) with check (id_perfil_finca = privado.mi_finca());

create policy "ver su finca" on public.chapeta for select to authenticated
  using (id_perfil_finca = privado.mi_finca());
create policy "crear en su finca" on public.chapeta for insert to authenticated
  with check (id_perfil_finca = privado.mi_finca());
create policy "editar en su finca" on public.chapeta for update to authenticated
  using (id_perfil_finca = privado.mi_finca()) with check (id_perfil_finca = privado.mi_finca());

create policy "ver su finca" on public.pesaje_pendiente for select to authenticated
  using (id_perfil_finca = privado.mi_finca());
create policy "crear en su finca" on public.pesaje_pendiente for insert to authenticated
  with check (id_perfil_finca = privado.mi_finca());
create policy "editar en su finca" on public.pesaje_pendiente for update to authenticated
  using (id_perfil_finca = privado.mi_finca()) with check (id_perfil_finca = privado.mi_finca());

-- Revisiones y siblings: cada uno a nombre de quien lo envía.
create policy "ver su finca" on public.revision for select to authenticated
  using (id_perfil_finca = privado.mi_finca());
create policy "crear en su finca" on public.revision for insert to authenticated
  with check (id_perfil_finca = privado.mi_finca() and id_usuario = auth.uid());

create policy "ver su finca" on public.sibling for select to authenticated
  using (id_perfil_finca = privado.mi_finca());
create policy "crear en su finca" on public.sibling for insert to authenticated
  with check (id_perfil_finca = privado.mi_finca() and id_usuario = auth.uid());
create policy "resolver en su finca" on public.sibling for update to authenticated
  using (id_perfil_finca = privado.mi_finca()) with check (id_perfil_finca = privado.mi_finca());
