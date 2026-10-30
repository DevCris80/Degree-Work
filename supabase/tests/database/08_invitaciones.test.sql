-- Invitaciones (CU-04, RF-05, RN-18): el Ganadero genera un código de un solo uso que vence a los 7 días, y quien lo
-- trae al crear su cuenta queda como Operario de esa finca, o no se crea nada.
begin;
-- En la nube el CLI entra con un rol temporal; las pruebas corren como postgres, igual que en local.
set local role postgres;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions;
select plan(25);

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-0000000000a1', 'ganadero.a@test'),
  ('00000000-0000-0000-0000-0000000000a2', 'operario.a@test'),
  ('00000000-0000-0000-0000-0000000000a3', 'sinfinca.a@test');
insert into public.perfil_finca (id_perfil_finca, nombre_finca, fecha_modificacion) values
  ('0000000a-0000-0000-0000-000000000000', 'Finca A', now()),
  ('0000000b-0000-0000-0000-000000000000', 'Finca B', now());
insert into public.usuario (id_usuario, id_perfil_finca, nombre, rol, fecha_modificacion) values
  ('00000000-0000-0000-0000-0000000000a1', '0000000a-0000-0000-0000-000000000000', 'Ganadero A', 'Ganadero', now()),
  ('00000000-0000-0000-0000-0000000000a2', '0000000a-0000-0000-0000-000000000000', 'Operario A', 'Operario', now());

-- Generar: solo el Ganadero, y el código queda en su finca con 7 días de vigencia.
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}', true);
set local role authenticated;
select lives_ok($$select public.generar_invitacion()$$, 'el Ganadero genera un código');
select throws_ok($$select * from public.invitacion$$, '42501', null, 'las invitaciones no se leen por la API');
select throws_ok(
  $$insert into public.invitacion (id_perfil_finca, codigo, id_usuario_creador, fecha_vencimiento)
    values ('0000000a-0000-0000-0000-000000000000', 'ABCD2345', '00000000-0000-0000-0000-0000000000a1', now())$$,
  '42501', null, 'ni se escriben por la API');
select throws_ok($$select privado.canjear_invitacion('00000000-0000-0000-0000-0000000000a1', 'Ana', 'ABCD2345', '0.1-provisional')$$,
  '42501', null, 'canjear_invitacion no se llama directo por la API');

select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a2","role":"authenticated"}', true);
select throws_ok($$select public.generar_invitacion()$$, '42501', null, 'un Operario no genera códigos');

select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a3","role":"authenticated"}', true);
select throws_ok($$select public.generar_invitacion()$$, '42501', null, 'quien no tiene finca no genera códigos');

set local role anon;
select throws_ok($$select public.generar_invitacion()$$, '42501', null, 'sin sesión no se generan códigos');

set local role postgres;
select is((select count(*)::int from public.invitacion), 1, 'solo el Ganadero dejó un código');
select results_eq(
  $$select id_perfil_finca, id_usuario_creador, id_usuario_canje is null, fecha_vencimiento = fecha_creacion + interval '7 days'
    from public.invitacion$$,
  $$values ('0000000a-0000-0000-0000-000000000000'::uuid, '00000000-0000-0000-0000-0000000000a1'::uuid, true, true)$$,
  'el código es de su finca, está sin usar y vence a los 7 días');
select matches((select codigo from public.invitacion), '^[A-HJKMNP-Z2-9]{8}$',
  'son 8 caracteres sin los que se confunden al dictarlos');
select throws_ok(
  $$insert into public.invitacion (id_perfil_finca, codigo, id_usuario_creador, fecha_vencimiento)
    values ('0000000a-0000-0000-0000-000000000000', 'abcd2345', '00000000-0000-0000-0000-0000000000a1', now())$$,
  '23514', null, 'el código se guarda siempre en mayúsculas');

-- Canjear: el código llega con espacios y en minúsculas, y la persona queda como Operario de esa finca.
select lives_ok(
  format($f$insert into auth.users (id, email, raw_user_meta_data) values ('00000000-0000-0000-0000-0000000000c1', 'luis@test',
    jsonb_build_object('nombre', ' Luis ', 'codigo_invitacion', %L, 'version_autorizacion', '0.1-provisional'))$f$,
    ' ' || lower((select codigo from public.invitacion)) || ' '),
  'con un código vigente se crea la cuenta');
select results_eq(
  $$select id_perfil_finca, nombre, rol from public.usuario where id_usuario = '00000000-0000-0000-0000-0000000000c1'$$,
  $$values ('0000000a-0000-0000-0000-000000000000'::uuid, 'Luis', 'Operario')$$,
  'queda como Operario de la finca del código, sin espacios de más');
select results_eq(
  $$select version_texto from public.aceptacion_datos where id_usuario = '00000000-0000-0000-0000-0000000000c1'$$,
  $$values ('0.1-provisional')$$,
  'la aceptación del texto queda registrada');
select results_eq(
  $$select id_usuario_canje, fecha_canje is not null from public.invitacion$$,
  $$values ('00000000-0000-0000-0000-0000000000c1'::uuid, true)$$,
  'el código queda usado por esa persona');
select is((select count(*)::int from public.perfil_finca), 2, 'unirse no crea una finca nueva');

-- Un código ya usado se rechaza y no deja la identidad.
select throws_ok(
  format($f$insert into auth.users (id, email, raw_user_meta_data) values ('00000000-0000-0000-0000-0000000000c2', 'repetido@test',
    jsonb_build_object('nombre', 'Otro', 'codigo_invitacion', %L, 'version_autorizacion', '0.1-provisional'))$f$,
    (select codigo from public.invitacion)),
  '22023', null, 'un código ya usado se rechaza');

-- Vencido, inexistente, o sin aceptar el texto: tampoco se crea la cuenta.
insert into public.invitacion (id_perfil_finca, codigo, id_usuario_creador, fecha_vencimiento) values
  ('0000000a-0000-0000-0000-000000000000', 'VENCIDO2', '00000000-0000-0000-0000-0000000000a1', now() - interval '1 second'),
  ('0000000a-0000-0000-0000-000000000000', 'VIGENTE2', '00000000-0000-0000-0000-0000000000a1', now() + interval '1 day');
select throws_ok(
  $$insert into auth.users (id, email, raw_user_meta_data) values ('00000000-0000-0000-0000-0000000000c3', 'tarde@test',
    '{"nombre": "Tarde", "codigo_invitacion": "VENCIDO2", "version_autorizacion": "0.1-provisional"}')$$,
  '22023', null, 'un código con más de 7 días se rechaza');
select throws_ok(
  $$insert into auth.users (id, email, raw_user_meta_data) values ('00000000-0000-0000-0000-0000000000c4', 'falso@test',
    '{"nombre": "Falso", "codigo_invitacion": "NOEXISTE", "version_autorizacion": "0.1-provisional"}')$$,
  '22023', null, 'un código que no existe se rechaza');
select throws_ok(
  $$insert into auth.users (id, email, raw_user_meta_data) values ('00000000-0000-0000-0000-0000000000c5', 'sinver@test',
    '{"nombre": "Sin versión", "codigo_invitacion": "VIGENTE2"}')$$,
  '22023', null, 'sin aceptar el texto de autorización no se crea la cuenta');
select throws_ok(
  $$insert into auth.users (id, email, raw_user_meta_data) values ('00000000-0000-0000-0000-0000000000c6', 'ambas@test',
    '{"nombre": "Ambas", "nombre_finca": "Nueva", "codigo_invitacion": "VIGENTE2", "version_autorizacion": "0.1-provisional"}')$$,
  '22023', null, 'un alta no crea una finca y se une a otra a la vez');
select is(
  (select count(*)::int from auth.users where email in ('repetido@test', 'tarde@test', 'falso@test', 'sinver@test', 'ambas@test')), 0,
  'ningún alta rechazada deja la identidad');
select is((select count(*)::int from public.invitacion where codigo = 'VIGENTE2' and id_usuario_canje is null), 1,
  'y el código vigente sigue sin usar');

-- Los metadatos no deciden el rol ni la finca: solo el código.
select lives_ok(
  $$insert into auth.users (id, email, raw_user_meta_data) values ('00000000-0000-0000-0000-0000000000c7', 'colado@test',
    '{"nombre": "Colado", "codigo_invitacion": "VIGENTE2", "version_autorizacion": "0.1-provisional",
      "rol": "Ganadero", "id_perfil_finca": "0000000b-0000-0000-0000-000000000000"}')$$,
  'un alta con rol y finca en los metadatos también se crea');
select results_eq(
  $$select id_perfil_finca, rol from public.usuario where id_usuario = '00000000-0000-0000-0000-0000000000c7'$$,
  $$values ('0000000a-0000-0000-0000-000000000000'::uuid, 'Operario')$$,
  'pero siempre como Operario de la finca del código');

select * from finish();
rollback;
