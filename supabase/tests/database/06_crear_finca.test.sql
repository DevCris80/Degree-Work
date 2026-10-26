-- crear_finca (CU-01, RF-01, RF-04, RN-20, RNF-13): el alta de la identidad crea en la misma transacción la finca,
-- su primer Usuario Ganadero y la aceptación del texto de autorización, o no crea nada.
begin;
-- En la nube el CLI entra con un rol temporal; las pruebas corren como postgres, igual que en local.
set local role postgres;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions;
select plan(19);

insert into public.perfil_finca (id_perfil_finca, nombre_finca, fecha_modificacion) values
  ('0000000a-0000-0000-0000-000000000000', 'Finca A', now());

-- Un registro con los datos de la finca y una versión vigente (lo que hace Supabase Auth al dar de alta).
select lives_ok(
  $$insert into auth.users (id, email, raw_user_meta_data) values ('00000000-0000-0000-0000-0000000000c1', 'nueva@test',
    '{"nombre": " Ana ", "nombre_finca": " La Esperanza ", "version_autorizacion": "0.1-provisional"}')$$,
  'el alta de la identidad crea la finca');
select results_eq(
  $$select f.nombre_finca, u.nombre, u.rol from public.usuario u join public.perfil_finca f using (id_perfil_finca)
    where u.id_usuario = '00000000-0000-0000-0000-0000000000c1'$$,
  $$values ('La Esperanza', 'Ana', 'Ganadero')$$,
  'la persona queda como Ganadero de su finca nueva (RN-20), con los nombres sin espacios de más');
select results_eq(
  $$select version_texto, fecha_aceptacion is not null from public.aceptacion_datos
    where id_usuario = '00000000-0000-0000-0000-0000000000c1'$$,
  $$values ('0.1-provisional', true)$$,
  'la aceptación queda con la versión del texto y su fecha');

-- Los metadatos no deciden el rol ni la finca.
select lives_ok(
  $$insert into auth.users (id, email, raw_user_meta_data) values ('00000000-0000-0000-0000-0000000000c2', 'colada@test',
    '{"nombre": "Colado", "nombre_finca": "Otra", "version_autorizacion": "0.1-provisional",
      "rol": "Operario", "id_perfil_finca": "0000000a-0000-0000-0000-000000000000"}')$$,
  'un alta con rol y finca en los metadatos también se crea');
select results_eq(
  $$select u.rol, u.id_perfil_finca <> '0000000a-0000-0000-0000-000000000000' from public.usuario u
    where u.id_usuario = '00000000-0000-0000-0000-0000000000c2'$$,
  $$values ('Ganadero', true)$$,
  'pero siempre como Ganadero de una finca nueva: no se une a una finca ajena');

-- Sin una versión vigente aceptada, o sin nombres, no queda ni la identidad.
select throws_ok(
  $$insert into auth.users (id, email, raw_user_meta_data) values ('00000000-0000-0000-0000-0000000000d1', 'sin.version@test',
    '{"nombre": "Ana", "nombre_finca": "La Esperanza"}')$$,
  '22023', null, 'sin versión aceptada no se crea la cuenta');
select throws_ok(
  $$insert into auth.users (id, email, raw_user_meta_data) values ('00000000-0000-0000-0000-0000000000d2', 'version.falsa@test',
    '{"nombre": "Ana", "nombre_finca": "La Esperanza", "version_autorizacion": "x"}')$$,
  '22023', null, 'una versión que no es de ningún texto publicado no cuenta como aceptación');
select throws_ok(
  $$insert into auth.users (id, email, raw_user_meta_data) values ('00000000-0000-0000-0000-0000000000d3', 'sin.nombre@test',
    '{"nombre": "  ", "nombre_finca": "La Esperanza", "version_autorizacion": "0.1-provisional"}')$$,
  '22023', null, 'sin el nombre de la persona no se crea la cuenta');
select throws_ok(
  $$insert into auth.users (id, email, raw_user_meta_data) values ('00000000-0000-0000-0000-0000000000d4', 'sin.finca@test',
    '{"nombre": "Ana", "nombre_finca": "", "version_autorizacion": "0.1-provisional"}')$$,
  '22023', null, 'sin el nombre de la finca no se crea la cuenta');
select is(
  (select count(*)::int from auth.users where email like 'sin.%@test' or email = 'version.falsa@test'), 0,
  'un alta rechazada no deja la identidad');

-- Un alta sin datos de finca (un Operario, CU-04) no crea finca.
select lives_ok(
  $$insert into auth.users (id, email) values ('00000000-0000-0000-0000-0000000000e1', 'operario@test')$$,
  'un alta sin datos de finca se crea');
select is((select count(*)::int from public.usuario where id_usuario = '00000000-0000-0000-0000-0000000000e1'), 0,
  'y no crea ningún Usuario ni finca');

-- La aceptación y los textos no se leen ni se escriben por la API, y crear_finca no se llama directo.
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000c1","role":"authenticated"}', true);
set local role authenticated;
select results_eq($$select nombre_finca from public.perfil_finca$$, $$values ('La Esperanza')$$,
  'el Ganadero ve su finca recién creada');
select throws_ok($$select * from public.aceptacion_datos$$, '42501', null, 'la aceptación no se lee por la API');
select throws_ok(
  $$insert into public.aceptacion_datos (id_usuario, version_texto) values ('00000000-0000-0000-0000-0000000000c1', '0.1-provisional')$$,
  '42501', null, 'la aceptación no se escribe por la API');
select throws_ok($$select * from privado.texto_autorizacion$$, '42501', null, 'los textos no se leen por la API');
select throws_ok(
  $$select privado.crear_finca('00000000-0000-0000-0000-0000000000c1', 'Ana', 'Segunda', '0.1-provisional')$$,
  '42501', null, 'crear_finca no se llama por la API: una persona no se crea una segunda finca');

-- La tabla de aceptaciones exige una versión publicada aunque la escriba otro camino.
set local role postgres;
select throws_ok(
  $$insert into public.aceptacion_datos (id_usuario, version_texto) values ('00000000-0000-0000-0000-0000000000c1', 'x')$$,
  '23503', null, 'una aceptación de un texto no publicado se rechaza');
select is((select count(*)::int from public.perfil_finca where nombre_finca in ('La Esperanza', 'Segunda')), 1,
  'la primera persona tiene una sola finca');

select * from finish();
rollback;
