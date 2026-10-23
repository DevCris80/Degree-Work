-- Una cuenta solo ve y escribe las filas de su finca, y una sin sesión no ve nada.
begin;
-- En la nube el CLI entra con un rol temporal; las pruebas corren como postgres, igual que en local.
set local role postgres;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions;
select plan(13);

-- Finca A con su Ganadero; finca B con el suyo. Los datos de A los escribe postgres, que no pasa por RLS.
insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-0000000000a1', 'ganadero.a@test'),
  ('00000000-0000-0000-0000-0000000000b1', 'ganadero.b@test');
insert into public.perfil_finca (id_perfil_finca, nombre_finca, fecha_modificacion) values
  ('0000000a-0000-0000-0000-000000000000', 'Finca A', now()),
  ('0000000b-0000-0000-0000-000000000000', 'Finca B', now());
insert into public.usuario (id_usuario, id_perfil_finca, nombre, rol, fecha_modificacion) values
  ('00000000-0000-0000-0000-0000000000a1', '0000000a-0000-0000-0000-000000000000', 'Ganadero A', 'Ganadero', now()),
  ('00000000-0000-0000-0000-0000000000b1', '0000000b-0000-0000-0000-000000000000', 'Ganadero B', 'Ganadero', now());
insert into public.animal (id_animal, id_perfil_finca, nombre, raza, sexo, etapa, fecha_nacimiento, proposito, fecha_modificacion) values
  ('a0000000-0000-0000-0000-000000000001', '0000000a-0000-0000-0000-000000000000', 'Lucero', 'Brahman', 'Hembra', 'Vaca', now(), 'Leche', now());
insert into public.evento (id_evento, id_animal, id_usuario, tipo_evento, fecha, detalle, fecha_modificacion) values
  ('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a1', 'Vacunación', now(), 'Aftosa', now());
insert into public.registro (id_registro, id_animal, id_usuario, id_lectura, peso, fecha_medicion, fecha_modificacion) values
  ('f0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a1', 'lectura-1', 410, now(), now());
insert into public.chapeta (id_chapeta, codigo, id_animal, fecha_asociacion, fecha_modificacion) values
  ('c0000000-0000-0000-0000-000000000001', '04A31B2C', 'a0000000-0000-0000-0000-000000000001', now(), now());
insert into public.pesaje_pendiente (id_pesaje_pendiente, id_lectura, codigo, peso, fecha_medicion, id_usuario, motivo, fecha_modificacion) values
  ('d0000000-0000-0000-0000-000000000001', 'lectura-2', '0BADC0DE', 380, now(), '00000000-0000-0000-0000-0000000000a1', 'CODIGO_SIN_CHAPETA_ACTIVA', now());

-- El Ganadero A ve lo suyo.
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}', true);
set local role authenticated;
select is((select count(*) from public.animal), 1::bigint, 'el Ganadero A ve el animal de su finca');
set local role postgres;

-- El Ganadero B no ve nada de la finca A.
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000b1","role":"authenticated"}', true);
set local role authenticated;
select is((select count(*) from public.animal), 0::bigint, 'B no ve animales de A');
select is((select count(*) from public.evento), 0::bigint, 'B no ve eventos de A');
select is((select count(*) from public.registro), 0::bigint, 'B no ve registros de A');
select is((select count(*) from public.chapeta), 0::bigint, 'B no ve chapetas de A');
select is((select count(*) from public.pesaje_pendiente), 0::bigint, 'B no ve pesajes pendientes de A');
select is((select array_agg(nombre) from public.usuario), array['Ganadero B'], 'B solo ve los Usuarios de su finca');
select is((select array_agg(nombre_finca) from public.perfil_finca), array['Finca B'], 'B solo ve su finca');

select throws_ok(
  $$insert into public.animal (id_animal, id_perfil_finca, nombre, raza, sexo, etapa, fecha_nacimiento, proposito, fecha_modificacion)
    values ('a0000000-0000-0000-0000-000000000099', '0000000a-0000-0000-0000-000000000000', 'Intruso', 'Cebú', 'Macho', 'Toro', now(), 'Carne', now())$$,
  '42501', null, 'B no puede crear animales en la finca A');
select throws_ok(
  $$insert into public.evento (id_evento, id_animal, id_usuario, tipo_evento, fecha, detalle, fecha_modificacion)
    values ('e0000000-0000-0000-0000-000000000099', 'a0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000b1', 'Otro', now(), '', now())$$,
  '23503', null, 'B no puede registrar eventos en un animal de A');
update public.animal set raza = 'Cambiada por B' where id_animal = 'a0000000-0000-0000-0000-000000000001';
set local role postgres;
select is((select raza from public.animal where id_animal = 'a0000000-0000-0000-0000-000000000001'), 'Brahman', 'B no puede modificar un animal de A');

-- Sin sesión no se ve nada.
set local role anon;
select throws_ok('select count(*) from public.animal', '42501', null, 'anon no puede leer animales');
select throws_ok('select count(*) from public.usuario', '42501', null, 'anon no puede leer usuarios');
set local role postgres;

select * from finish();
rollback;
