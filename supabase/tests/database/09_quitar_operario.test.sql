-- Quitar a un Operario (CU-05, RF-37, LIM-06): solo el Ganadero de su finca, el Operario quitado pierde el acceso a
-- la finca y sus registros anteriores se conservan a su nombre.
begin;
-- En la nube el CLI entra con un rol temporal; las pruebas corren como postgres, igual que en local.
set local role postgres;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions;
-- Estas pruebas escriben directo en las tablas, como lo hace subir_cambios por dentro (04_subir_cambios).
select set_config('miganado.subir_cambios', 'si', true);
select plan(18);

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-0000000000a1', 'ganadero.a@test'),
  ('00000000-0000-0000-0000-0000000000a2', 'operario.a@test'),
  ('00000000-0000-0000-0000-0000000000a3', 'otro.operario.a@test'),
  ('00000000-0000-0000-0000-0000000000b1', 'ganadero.b@test');
insert into public.perfil_finca (id_perfil_finca, nombre_finca, fecha_modificacion) values
  ('0000000a-0000-0000-0000-000000000000', 'Finca A', now()),
  ('0000000b-0000-0000-0000-000000000000', 'Finca B', now());
insert into public.usuario (id_usuario, id_perfil_finca, nombre, rol, fecha_modificacion) values
  ('00000000-0000-0000-0000-0000000000a1', '0000000a-0000-0000-0000-000000000000', 'Ganadero A', 'Ganadero', now()),
  ('00000000-0000-0000-0000-0000000000a2', '0000000a-0000-0000-0000-000000000000', 'Operario A', 'Operario', now()),
  ('00000000-0000-0000-0000-0000000000a3', '0000000a-0000-0000-0000-000000000000', 'Otro Operario A', 'Operario', now()),
  ('00000000-0000-0000-0000-0000000000b1', '0000000b-0000-0000-0000-000000000000', 'Ganadero B', 'Ganadero', now());
insert into public.animal (id_animal, id_perfil_finca, nombre, raza, sexo, etapa, fecha_nacimiento, proposito, fecha_modificacion) values
  ('a0000000-0000-0000-0000-000000000001', '0000000a-0000-0000-0000-000000000000', 'Lucero', 'Brahman', 'Hembra', 'Vaca', now(), 'Leche', now());
insert into public.registro (id_registro, id_animal, id_usuario, id_lectura, peso, fecha_medicion, fecha_modificacion) values
  ('f0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a2', 'lectura-1', 410, now(), now());

-- Quien no es Ganadero de la finca no quita a nadie.
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a3","role":"authenticated"}', true);
set local role authenticated;
select throws_ok($$select public.quitar_operario('00000000-0000-0000-0000-0000000000a2')$$,
  '42501', null, 'un Operario no quita a otro Operario');

set local role anon;
select throws_ok($$select public.quitar_operario('00000000-0000-0000-0000-0000000000a2')$$,
  '42501', null, 'sin sesión no se quita a nadie');

select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000b1","role":"authenticated"}', true);
set local role authenticated;
select throws_ok($$select public.quitar_operario('00000000-0000-0000-0000-0000000000a2')$$,
  '22023', null, 'el Ganadero de otra finca no quita a un Operario que no es suyo');

-- El Ganadero de la finca quita a su Operario.
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}', true);
select throws_ok($$select public.quitar_operario('00000000-0000-0000-0000-0000000000a1')$$,
  '22023', null, 'no se quita a un Ganadero');
select throws_ok($$select public.quitar_operario('00000000-0000-0000-0000-0000000000ff')$$,
  '22023', null, 'no se quita a quien no existe');
select lives_ok($$select public.quitar_operario('00000000-0000-0000-0000-0000000000a2')$$,
  'el Ganadero quita a un Operario de su finca');
select throws_ok($$select public.quitar_operario('00000000-0000-0000-0000-0000000000a2')$$,
  '22023', null, 'un Operario ya quitado no se quita otra vez');

select is((select count(*)::int from public.registro where id_usuario = '00000000-0000-0000-0000-0000000000a2'), 1,
  'sus pesajes siguen a su nombre y a la vista del Ganadero');
select is((select count(*)::int from public.usuario), 3,
  'su fila de Usuario se conserva');

set local role postgres;
select ok((select fecha_quitado is not null from public.usuario where id_usuario = '00000000-0000-0000-0000-0000000000a2'),
  'el Operario queda marcado con la fecha en que lo quitaron');
select is((select count(*)::int from public.usuario where fecha_quitado is not null), 1,
  'y nadie más queda marcado');

-- El Operario quitado ya no pertenece a ninguna finca.
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a2","role":"authenticated"}', true);
set local role authenticated;
select is(public.sigo_en_la_finca(), false, 'el Operario quitado sabe que ya no está en la finca');
select is((select count(*)::int from public.animal), 0, 'el Operario quitado no lee los Animales de la finca');
select is((select count(*)::int from public.usuario), 0, 'ni sus Usuarios');
select throws_ok(
  $$insert into public.animal (id_animal, id_perfil_finca, nombre, raza, sexo, etapa, fecha_nacimiento, proposito, fecha_modificacion)
    values ('a0000000-0000-0000-0000-000000000002', '0000000a-0000-0000-0000-000000000000', 'Mariposa', 'Gyr', 'Hembra', 'Ternera', now(), 'Leche', now())$$,
  '42501', null, 'el Operario quitado no escribe en la finca');
select throws_ok($$select public.generar_invitacion()$$,
  '42501', null, 'el Operario quitado no genera códigos de invitación');

-- El otro Operario sigue dentro.
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a3","role":"authenticated"}', true);
select is(public.sigo_en_la_finca(), true, 'el otro Operario sigue en la finca');
select is((select count(*)::int from public.animal), 1, 'y sigue viendo sus Animales');

select * from finish();
rollback;
