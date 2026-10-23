-- Unicidad (RN-01, RN-09, RN-21), cursor de descarga, estados definitivos y datos válidos.
begin;
select plan(21);

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
  ('a0000000-0000-0000-0000-000000000001', '0000000a-0000-0000-0000-000000000000', 'Lucero', 'Brahman', 'Hembra', 'Vaca', now(), 'Leche', now()),
  ('a0000000-0000-0000-0000-000000000002', '0000000a-0000-0000-0000-000000000000', 'Mariposa', 'Gyr', 'Hembra', 'Ternera', now(), 'Leche', now()),
  ('b0000000-0000-0000-0000-000000000001', '0000000b-0000-0000-0000-000000000000', 'Toro B', 'Cebú', 'Macho', 'Toro', now(), 'Carne', now());
insert into public.chapeta (id_chapeta, codigo, id_animal, fecha_asociacion, fecha_modificacion) values
  ('c0000000-0000-0000-0000-000000000001', '04A31B2C', 'a0000000-0000-0000-0000-000000000001', now(), now());
insert into public.registro (id_registro, id_animal, id_usuario, id_lectura, peso, fecha_medicion, fecha_modificacion) values
  ('f0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a1', 'lectura-1', 410, now(), now());
insert into public.pesaje_pendiente (id_pesaje_pendiente, id_lectura, codigo, peso, fecha_medicion, id_usuario, motivo, resolucion, id_usuario_resolucion, fecha_resolucion, fecha_modificacion) values
  ('d0000000-0000-0000-0000-000000000001', 'lectura-2', '0BADC0DE', 380, now(), '00000000-0000-0000-0000-0000000000a1', 'CODIGO_SIN_CHAPETA_ACTIVA', 'ASIGNADO', '00000000-0000-0000-0000-0000000000a1', now(), now());

-- Unicidad
select throws_ok(
  $$insert into public.chapeta (id_chapeta, codigo, id_animal, fecha_asociacion, fecha_modificacion)
    values ('c0000000-0000-0000-0000-000000000002', '04A31B2C', 'a0000000-0000-0000-0000-000000000002', now(), now())$$,
  '23505', null, 'un código no tiene dos Chapetas activas en la finca');
select lives_ok(
  $$insert into public.chapeta (id_chapeta, codigo, id_animal, fecha_asociacion, fecha_modificacion)
    values ('c0000000-0000-0000-0000-000000000003', '04A31B2C', 'b0000000-0000-0000-0000-000000000001', now(), now())$$,
  'otra finca puede tener activo el mismo código');
update public.chapeta set fecha_desasociacion = now() where id_chapeta = 'c0000000-0000-0000-0000-000000000001';
select lives_ok(
  $$insert into public.chapeta (id_chapeta, codigo, id_animal, fecha_asociacion, fecha_modificacion)
    values ('c0000000-0000-0000-0000-000000000004', '04A31B2C', 'a0000000-0000-0000-0000-000000000002', now(), now())$$,
  'un código liberado se puede volver a asociar');
select throws_ok(
  $$insert into public.registro (id_registro, id_animal, id_usuario, id_lectura, peso, fecha_medicion, fecha_modificacion)
    values ('f0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-0000000000a1', 'lectura-1', 300, now(), now())$$,
  '23505', null, 'un pesaje del ESP32 es un solo Registro');
select throws_ok(
  $$insert into public.pesaje_pendiente (id_pesaje_pendiente, id_lectura, codigo, peso, fecha_medicion, id_usuario, motivo, fecha_modificacion)
    values ('d0000000-0000-0000-0000-000000000002', 'lectura-2', '0BADC0DE', 380, now(), '00000000-0000-0000-0000-0000000000a1', 'CODIGO_SIN_CHAPETA_ACTIVA', now())$$,
  '23505', null, 'un pesaje del ESP32 es un solo Pesaje pendiente');

-- Cursor de descarga
create temp table cursor_antes as
  select cursor_descarga from public.animal where id_animal = 'a0000000-0000-0000-0000-000000000001';
update public.animal set raza = 'Brahman rojo', cursor_descarga = 1 where id_animal = 'a0000000-0000-0000-0000-000000000001';
select cmp_ok(
  (select cursor_descarga from public.animal where id_animal = 'a0000000-0000-0000-0000-000000000001'),
  '>', (select cursor_descarga from cursor_antes), 'modificar una fila avanza su cursor, aunque el cliente envíe otro');
select cmp_ok(
  (select cursor_descarga from public.animal where id_animal = 'a0000000-0000-0000-0000-000000000001'),
  '>', (select max(cursor_descarga) from public.chapeta),
  'el cursor es global: el Animal modificado después de las Chapetas queda por encima de todas');

-- Estados definitivos
update public.animal set fecha_baja = now() where id_animal = 'a0000000-0000-0000-0000-000000000002';
select throws_ok(
  $$update public.animal set fecha_baja = null where id_animal = 'a0000000-0000-0000-0000-000000000002'$$,
  '55000', null, 'la baja de un Animal es definitiva');
update public.registro set fecha_baja = now() where id_registro = 'f0000000-0000-0000-0000-000000000001';
select throws_ok(
  $$update public.registro set fecha_baja = null where id_registro = 'f0000000-0000-0000-0000-000000000001'$$,
  '55000', null, 'la baja de un Registro es definitiva');
select throws_ok(
  $$update public.registro set id_lectura = 'otra' where id_registro = 'f0000000-0000-0000-0000-000000000001'$$,
  '55000', null, 'un Registro conserva su id_lectura');
select throws_ok(
  $$update public.chapeta set fecha_desasociacion = null where id_chapeta = 'c0000000-0000-0000-0000-000000000001'$$,
  '55000', null, 'liberar una Chapeta es definitivo');
select throws_ok(
  $$update public.chapeta set codigo = '04A31B2F' where id_chapeta = 'c0000000-0000-0000-0000-000000000004'$$,
  '55000', null, 'el código de una Chapeta no cambia');
select throws_ok(
  $$update public.pesaje_pendiente set resolucion = 'DESCARTADO' where id_pesaje_pendiente = 'd0000000-0000-0000-0000-000000000001'$$,
  '55000', null, 'conciliar es definitivo');
select throws_ok(
  $$update public.pesaje_pendiente set peso = 999 where id_pesaje_pendiente = 'd0000000-0000-0000-0000-000000000001'$$,
  '55000', null, 'el peso de un Pesaje pendiente no cambia');
select throws_ok(
  $$update public.animal set id_perfil_finca = '0000000b-0000-0000-0000-000000000000' where id_animal = 'a0000000-0000-0000-0000-000000000001'$$,
  '55000', null, 'un Animal no cambia de finca');
insert into public.sibling (id_version, entidad, id_fila, id_perfil_finca, rev_base, contenido, id_usuario) values
  ('5a000000-0000-0000-0000-000000000001', 'animal', 'a0000000-0000-0000-0000-000000000001', '0000000a-0000-0000-0000-000000000000', 1, '{}', '00000000-0000-0000-0000-0000000000a1');
select throws_ok(
  $$update public.sibling set contenido = '{"raza":"otra"}' where id_version = '5a000000-0000-0000-0000-000000000001'$$,
  '55000', null, 'el contenido de un sibling no cambia');
update public.sibling set resuelto_por = gen_random_uuid(), fecha_resolucion = now()
  where id_version = '5a000000-0000-0000-0000-000000000001';
select throws_ok(
  $$update public.sibling set resuelto_por = gen_random_uuid() where id_version = '5a000000-0000-0000-0000-000000000001'$$,
  '55000', null, 'un sibling resuelto no cambia');

-- Datos válidos
select throws_ok(
  $$insert into public.animal (id_animal, id_perfil_finca, nombre, raza, sexo, etapa, fecha_nacimiento, proposito, fecha_modificacion)
    values ('a0000000-0000-0000-0000-000000000009', '0000000a-0000-0000-0000-000000000000', 'X', 'Y', 'Macho', 'Vaca', now(), 'Carne', now())$$,
  '23514', null, 'la etapa corresponde al sexo');
select throws_ok(
  $$insert into public.chapeta (id_chapeta, codigo, id_animal, fecha_asociacion, fecha_modificacion)
    values ('c0000000-0000-0000-0000-000000000009', '04:a3:1b', 'a0000000-0000-0000-0000-000000000001', now(), now())$$,
  '23514', null, 'el código se guarda normalizado');
select throws_ok(
  $$insert into public.registro (id_registro, id_animal, id_usuario, peso, fecha_medicion, fecha_modificacion)
    values ('f0000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a1', 1600, now(), now())$$,
  '23514', null, 'el peso está entre 0 y 1500 kg');
select throws_ok(
  $$insert into public.animal (id_animal, id_perfil_finca, id_padre, nombre, raza, sexo, etapa, fecha_nacimiento, proposito, fecha_modificacion)
    values ('a0000000-0000-0000-0000-000000000008', '0000000a-0000-0000-0000-000000000000', 'b0000000-0000-0000-0000-000000000001', 'Cría', 'Cebú', 'Macho', 'Novillo', now(), 'Carne', now())$$,
  '23503', null, 'el padre es de la misma finca');

select * from finish();
rollback;
