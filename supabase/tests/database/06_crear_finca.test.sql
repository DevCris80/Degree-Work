-- crear_finca (CU-01, RF-01, RF-04, RN-20, RNF-13): la finca, su primer Usuario Ganadero y la aceptación del
-- texto de autorización se crean juntos, y nunca dos fincas para la misma persona.
begin;
-- En la nube el CLI entra con un rol temporal; las pruebas corren como postgres, igual que en local.
set local role postgres;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions;
select plan(18);

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-0000000000c1', 'nueva@test'),
  ('00000000-0000-0000-0000-0000000000a2', 'operario.a@test');
insert into public.perfil_finca (id_perfil_finca, nombre_finca, fecha_modificacion) values
  ('0000000a-0000-0000-0000-000000000000', 'Finca A', now());
insert into public.usuario (id_usuario, id_perfil_finca, nombre, rol, fecha_modificacion) values
  ('00000000-0000-0000-0000-0000000000a2', '0000000a-0000-0000-0000-000000000000', 'Operario A', 'Operario', now());

-- Sin sesión no se crea nada.
set local role anon;
select throws_ok(
  $$select public.crear_finca('Ana', 'La Esperanza', '0.1-provisional')$$,
  '42501', null, 'sin sesión no se crea ninguna finca');

-- Una persona recién registrada, sin finca.
set local role postgres;
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000c1","role":"authenticated"}', true);
set local role authenticated;

select throws_ok(
  $$select public.crear_finca('Ana', 'La Esperanza', null)$$,
  '22023', null, 'sin la versión del texto aceptado no se crea la finca');
select throws_ok(
  $$select public.crear_finca('Ana', 'La Esperanza', '  ')$$,
  '22023', null, 'una versión en blanco no cuenta como aceptación');
select throws_ok(
  $$select public.crear_finca('  ', 'La Esperanza', '0.1-provisional')$$,
  '22023', null, 'sin el nombre de la persona no se crea la finca');
select throws_ok(
  $$select public.crear_finca('Ana', '', '0.1-provisional')$$,
  '22023', null, 'sin el nombre de la finca no se crea');

create temp table creada as select public.crear_finca(' Ana ', ' La Esperanza ', '0.1-provisional') as respuesta;
grant select on creada to authenticated, anon;

select is((select respuesta->'perfil_finca'->>'nombre_finca' from creada), 'La Esperanza',
  'la respuesta trae la finca, con el nombre sin espacios de más');
select is((select respuesta->'usuario'->>'id_usuario' from creada), '00000000-0000-0000-0000-0000000000c1',
  'el Usuario es la persona de la sesión');
select is((select respuesta->'usuario'->>'rol' from creada), 'Ganadero', 'el primer Usuario es Ganadero (RN-20)');
select is((select respuesta->'usuario'->>'nombre' from creada), 'Ana', 'el Usuario lleva el nombre sin espacios de más');
select is((select respuesta->'usuario'->>'id_perfil_finca' from creada), (select respuesta->'perfil_finca'->>'id_perfil_finca' from creada),
  'el Usuario queda en la finca creada');
select ok((select (respuesta->'perfil_finca') ? 'fecha_modificacion' from creada), 'la finca trae su fecha de modificación');
select results_eq(
  $$select nombre_finca from public.perfil_finca$$, $$values ('La Esperanza')$$,
  'el Ganadero ve su finca recién creada');

-- Reintentar no crea una segunda finca: devuelve la misma.
select is(
  (select public.crear_finca('Ana', 'Otra finca', '0.1-provisional')->'perfil_finca'->>'id_perfil_finca'),
  (select respuesta->'perfil_finca'->>'id_perfil_finca' from creada),
  'un segundo intento devuelve la misma finca');

select throws_ok($$select * from public.aceptacion_datos$$, '42501', null, 'la aceptación no se lee por la API');
select throws_ok(
  $$insert into public.aceptacion_datos (id_usuario, version_texto) values ('00000000-0000-0000-0000-0000000000c1', 'falsa')$$,
  '42501', null, 'la aceptación no se escribe por la API');

-- Un Operario que ya pertenece a una finca no crea otra.
set local role postgres;
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a2","role":"authenticated"}', true);
set local role authenticated;
select is(
  (select public.crear_finca('Operario A', 'Finca propia', '0.1-provisional')->'perfil_finca'->>'id_perfil_finca'),
  '0000000a-0000-0000-0000-000000000000',
  'quien ya tiene finca recibe la suya, sin crear otra');

set local role postgres;
select results_eq(
  $$select count(*)::int from public.perfil_finca where nombre_finca in ('La Esperanza', 'Otra finca', 'Finca propia')$$,
  $$values (1)$$,
  'en total se creó una sola finca');
select results_eq(
  $$select id_usuario::text, version_texto from public.aceptacion_datos$$,
  $$values ('00000000-0000-0000-0000-0000000000c1', '0.1-provisional')$$,
  'la aceptación quedó una vez, con la versión del texto (y su fecha, que no puede ser nula)');

select * from finish();
rollback;
