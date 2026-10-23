-- El Operario no da de baja, no libera ni descarta; nadie borra; nadie escribe Usuarios por la API.
begin;
select plan(28);

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-0000000000a1', 'ganadero.a@test'),
  ('00000000-0000-0000-0000-0000000000a2', 'operario.a@test');
insert into public.perfil_finca (id_perfil_finca, nombre_finca, fecha_modificacion) values
  ('0000000a-0000-0000-0000-000000000000', 'Finca A', now());
insert into public.usuario (id_usuario, id_perfil_finca, nombre, rol, fecha_modificacion) values
  ('00000000-0000-0000-0000-0000000000a1', '0000000a-0000-0000-0000-000000000000', 'Ganadero A', 'Ganadero', now()),
  ('00000000-0000-0000-0000-0000000000a2', '0000000a-0000-0000-0000-000000000000', 'Operario A', 'Operario', now());
insert into public.animal (id_animal, id_perfil_finca, nombre, raza, sexo, etapa, fecha_nacimiento, proposito, fecha_modificacion) values
  ('a0000000-0000-0000-0000-000000000001', '0000000a-0000-0000-0000-000000000000', 'Lucero', 'Brahman', 'Hembra', 'Vaca', now(), 'Leche', now()),
  ('a0000000-0000-0000-0000-000000000002', '0000000a-0000-0000-0000-000000000000', 'Mariposa', 'Gyr', 'Hembra', 'Ternera', now(), 'Leche', now());
insert into public.chapeta (id_chapeta, codigo, id_animal, fecha_asociacion, fecha_modificacion) values
  ('c0000000-0000-0000-0000-000000000001', '04A31B2C', 'a0000000-0000-0000-0000-000000000001', now(), now()),
  ('c0000000-0000-0000-0000-000000000002', '04A31B2D', 'a0000000-0000-0000-0000-000000000002', now(), now());
insert into public.evento (id_evento, id_animal, id_usuario, tipo_evento, fecha, detalle, fecha_modificacion) values
  ('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a2', 'Vacunación', now(), 'Aftosa', now());
insert into public.pesaje_pendiente (id_pesaje_pendiente, id_lectura, codigo, peso, fecha_medicion, id_usuario, motivo, fecha_modificacion) values
  ('d0000000-0000-0000-0000-000000000001', 'lectura-1', '0BADC0DE', 380, now(), '00000000-0000-0000-0000-0000000000a1', 'CODIGO_SIN_CHAPETA_ACTIVA', now()),
  ('d0000000-0000-0000-0000-000000000002', 'lectura-2', '0BADC0DE', 381, now(), '00000000-0000-0000-0000-0000000000a1', 'CODIGO_SIN_CHAPETA_ACTIVA', now()),
  ('d0000000-0000-0000-0000-000000000003', 'lectura-3', '0BADC0DE', 382, now(), '00000000-0000-0000-0000-0000000000a1', 'CODIGO_SIN_CHAPETA_ACTIVA', now());

-- Operario
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a2","role":"authenticated"}', true);
set local role authenticated;

select throws_ok(
  $$update public.animal set fecha_baja = now() where id_animal = 'a0000000-0000-0000-0000-000000000001'$$,
  '42501', null, 'el Operario no puede dar de baja un Animal');
select throws_ok(
  $$insert into public.animal (id_animal, id_perfil_finca, nombre, raza, sexo, etapa, fecha_nacimiento, proposito, fecha_baja, fecha_modificacion)
    values ('a0000000-0000-0000-0000-000000000003', '0000000a-0000-0000-0000-000000000000', 'Vendido', 'Cebú', 'Macho', 'Toro', now(), 'Carne', now(), now())$$,
  '42501', null, 'el Operario no puede subir un Animal ya dado de baja');
select lives_ok(
  $$update public.animal set raza = 'Brahman rojo' where id_animal = 'a0000000-0000-0000-0000-000000000001'$$,
  'el Operario puede editar un Animal');
select throws_ok(
  $$update public.chapeta set fecha_desasociacion = now() where id_chapeta = 'c0000000-0000-0000-0000-000000000001'$$,
  '42501', null, 'el Operario no puede liberar una Chapeta');
select throws_ok(
  $$update public.pesaje_pendiente
    set resolucion = 'DESCARTADO', id_usuario_resolucion = '00000000-0000-0000-0000-0000000000a2', fecha_resolucion = now()
    where id_pesaje_pendiente = 'd0000000-0000-0000-0000-000000000001'$$,
  '42501', null, 'el Operario no puede descartar un Pesaje pendiente');
select lives_ok(
  $$insert into public.registro (id_registro, id_animal, id_usuario, id_lectura, peso, fecha_medicion, fecha_modificacion)
    values ('f0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a1', 'lectura-2', 381, now(), now())$$,
  'al conciliar, el Registro conserva el autor del Pesaje pendiente');
select lives_ok(
  $$update public.pesaje_pendiente
    set resolucion = 'ASIGNADO', id_usuario_resolucion = '00000000-0000-0000-0000-0000000000a2', fecha_resolucion = now()
    where id_pesaje_pendiente = 'd0000000-0000-0000-0000-000000000002'$$,
  'el Operario puede asignar un Pesaje pendiente');
set constraints all immediate;
select throws_ok(
  $$update public.pesaje_pendiente
    set resolucion = 'ASIGNADO', id_usuario_resolucion = '00000000-0000-0000-0000-0000000000a2', fecha_resolucion = now()
    where id_pesaje_pendiente = 'd0000000-0000-0000-0000-000000000001'$$,
  '23503', null, 'asignar un Pesaje pendiente sin su Registro no es descartarlo a escondidas');
set constraints all deferred;
select throws_ok(
  $$update public.pesaje_pendiente
    set resolucion = 'ASIGNADO', id_usuario_resolucion = '00000000-0000-0000-0000-0000000000a1', fecha_resolucion = now()
    where id_pesaje_pendiente = 'd0000000-0000-0000-0000-000000000001'$$,
  '42501', null, 'la conciliación queda a nombre de quien concilia');
select throws_ok(
  $$insert into public.evento (id_evento, id_animal, id_usuario, tipo_evento, fecha, detalle, fecha_modificacion)
    values ('e0000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a1', 'Otro', now(), '', now())$$,
  '42501', null, 'el Operario no registra un Evento a nombre de otro');
select throws_ok(
  $$insert into public.registro (id_registro, id_animal, id_usuario, peso, fecha_medicion, fecha_modificacion)
    values ('f0000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a1', 300, now(), now())$$,
  '42501', null, 'el Operario no registra un pesaje manual a nombre de otro');
select throws_ok(
  $$update public.evento set fecha_baja = now() where id_evento = 'e0000000-0000-0000-0000-000000000001'$$,
  '42501', null, 'el Operario no puede dar de baja un Evento, ni uno suyo');
select throws_ok(
  $$update public.registro set fecha_baja = now() where id_registro = 'f0000000-0000-0000-0000-000000000002'$$,
  '42501', null, 'el Operario no puede dar de baja un Registro');
select throws_ok(
  $$insert into public.evento (id_evento, id_animal, id_usuario, tipo_evento, fecha, detalle, fecha_baja, fecha_modificacion)
    values ('e0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a2', 'Otro', now(), '', now(), now())$$,
  '42501', null, 'el Operario no puede subir un Evento ya dado de baja');
update public.perfil_finca set nombre_finca = 'Cambiada por el Operario';
select is((select nombre_finca from public.perfil_finca), 'Finca A', 'el Operario no puede editar la finca');
reset role;

-- Ganadero
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}', true);
set local role authenticated;

select lives_ok(
  $$update public.animal set fecha_baja = now() where id_animal = 'a0000000-0000-0000-0000-000000000002'$$,
  'el Ganadero puede dar de baja un Animal');
select lives_ok(
  $$update public.chapeta set fecha_desasociacion = now() where id_chapeta = 'c0000000-0000-0000-0000-000000000002'$$,
  'el Ganadero puede liberar una Chapeta');
select lives_ok(
  $$update public.evento set fecha_baja = now() where id_evento = 'e0000000-0000-0000-0000-000000000001'$$,
  'el Ganadero puede dar de baja un Evento');
select lives_ok(
  $$update public.pesaje_pendiente
    set resolucion = 'DESCARTADO', id_usuario_resolucion = '00000000-0000-0000-0000-0000000000a1', fecha_resolucion = now()
    where id_pesaje_pendiente = 'd0000000-0000-0000-0000-000000000003'$$,
  'el Ganadero puede descartar un Pesaje pendiente');
update public.perfil_finca set nombre_finca = 'La Esperanza';
select is((select nombre_finca from public.perfil_finca), 'La Esperanza', 'el Ganadero puede editar su finca');

select throws_ok($$delete from public.animal$$, '42501', null, 'ni el Ganadero borra Animales');
select throws_ok($$delete from public.evento$$, '42501', null, 'ni el Ganadero borra Eventos');
select throws_ok($$delete from public.registro$$, '42501', null, 'ni el Ganadero borra Registros');
select throws_ok($$delete from public.chapeta$$, '42501', null, 'ni el Ganadero borra Chapetas');
select throws_ok($$delete from public.pesaje_pendiente$$, '42501', null, 'ni el Ganadero borra Pesajes pendientes');
select throws_ok(
  $$update public.usuario set rol = 'Ganadero' where id_usuario = '00000000-0000-0000-0000-0000000000a2'$$,
  '42501', null, 'los roles no se cambian por la API');
select throws_ok(
  $$insert into public.perfil_finca (id_perfil_finca, nombre_finca, fecha_modificacion)
    values ('0000000c-0000-0000-0000-000000000000', 'Otra finca', now())$$,
  '42501', null, 'las fincas no se crean por la API');
reset role;

-- Una tabla que agregue una migración futura tampoco se puede borrar desde la API.
create table public.tabla_futura (id integer);
select ok(not has_table_privilege('authenticated', 'public.tabla_futura', 'DELETE'),
  'las tablas nuevas no conceden DELETE a authenticated');

select * from finish();
rollback;
