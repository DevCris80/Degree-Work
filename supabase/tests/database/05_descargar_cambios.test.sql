-- descargar_cambios (CU-20 paso 7, RF-36, RNF-09): páginas por cursor, solo de la finca propia, con siblings y
-- con las filas a las que apunta cada página.
begin;
-- En la nube el CLI entra con un rol temporal; las pruebas corren como postgres, igual que en local.
set local role postgres;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions;
-- Los datos se escriben directo en las tablas, como lo hace subir_cambios por dentro (04_subir_cambios).
select set_config('miganado.subir_cambios', 'si', true);
select plan(18);

-- Las filas de esta prueba toman cursores mayores que este.
create temp table inicio as select nextval('privado.cursor_descarga') as cursor;
grant select on inicio to authenticated, anon;

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-0000000000a1', 'ganadero.a@test'),
  ('00000000-0000-0000-0000-0000000000a2', 'operario.a@test'),
  ('00000000-0000-0000-0000-0000000000b1', 'ganadero.b@test');
insert into public.perfil_finca (id_perfil_finca, nombre_finca, fecha_modificacion) values
  ('0000000a-0000-0000-0000-000000000000', 'Finca A', now()),
  ('0000000b-0000-0000-0000-000000000000', 'Finca B', now());
insert into public.usuario (id_usuario, id_perfil_finca, nombre, rol, fecha_modificacion) values
  ('00000000-0000-0000-0000-0000000000a1', '0000000a-0000-0000-0000-000000000000', 'Ganadero A', 'Ganadero', now()),
  ('00000000-0000-0000-0000-0000000000a2', '0000000a-0000-0000-0000-000000000000', 'Operario A', 'Operario', now()),
  ('00000000-0000-0000-0000-0000000000b1', '0000000b-0000-0000-0000-000000000000', 'Ganadero B', 'Ganadero', now());
-- Lucero es la madre de Lunita.
insert into public.animal (id_animal, id_perfil_finca, id_madre, nombre, raza, sexo, etapa, fecha_nacimiento, proposito, fecha_modificacion) values
  ('a0000000-0000-0000-0000-000000000001', '0000000a-0000-0000-0000-000000000000', null, 'Lucero', 'Holstein', 'Hembra', 'Vaca', now(), 'Leche', now()),
  ('a0000000-0000-0000-0000-000000000002', '0000000a-0000-0000-0000-000000000000', 'a0000000-0000-0000-0000-000000000001', 'Lunita', 'Holstein', 'Hembra', 'Ternera', now(), 'Leche', now()),
  ('b0000000-0000-0000-0000-000000000001', '0000000b-0000-0000-0000-000000000000', null, 'Tornado', 'Brahman', 'Macho', 'Toro', now(), 'Carne', now());
insert into public.chapeta (id_chapeta, codigo, id_animal, fecha_asociacion, fecha_modificacion) values
  ('c0000000-0000-0000-0000-000000000001', 'FP001', 'a0000000-0000-0000-0000-000000000001', now(), now());
insert into public.sibling (id_version, entidad, id_fila, id_perfil_finca, rev_base, contenido, id_usuario) values
  ('5a000000-0000-0000-0000-000000000001', 'animal', 'a0000000-0000-0000-0000-000000000001',
   '0000000a-0000-0000-0000-000000000000', 1, '{"raza": "Gyr"}', '00000000-0000-0000-0000-0000000000a2');
-- El Evento lo registró el Operario, y es lo último que se escribe en la finca A.
insert into public.evento (id_evento, id_animal, id_usuario, tipo_evento, fecha, detalle, biologico, dosis, via, fecha_modificacion) values
  ('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-0000000000a2',
   'Vacunación', now(), 'Aftosa', 'Aftosa', '2 ml', 'Subcutánea', now());

-- Una página desde `desde`, con un límite.
create function pg_temp.pagina(desde bigint, limite int default 100) returns jsonb
language sql as $$ select public.descargar_cambios(desde, limite) $$;
-- Las entidades de los cambios de una página, en orden.
create function pg_temp.entidades(pagina jsonb) returns text[]
language sql as $$ select array_agg(c ->> 'entidad' order by o) from jsonb_array_elements(pagina -> 'cambios') with ordinality t(c, o) $$;

-- Ganadero A
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}', true);
set local role authenticated;

-- Aislamiento y orden
select is(
  pg_temp.entidades(pg_temp.pagina((select cursor from inicio))),
  array['perfil_finca', 'usuario', 'usuario', 'animal', 'animal', 'chapeta', 'sibling', 'evento'],
  'trae las filas de su finca en el orden en que se escribieron, y nada de otra finca');
select ok(
  (select bool_and((c ->> 'cursor')::bigint > (select cursor from inicio))
   from jsonb_array_elements(pg_temp.pagina((select cursor from inicio)) -> 'cambios') c),
  'cada cambio trae su cursor');
select ok(
  pg_temp.pagina((select cursor from inicio)) -> 'cambios' -> 3 @> '{"entidad": "animal", "fila": {"nombre": "Lucero", "rev": 1}}',
  'cada cambio trae la fila completa');
select ok(
  pg_temp.pagina((select cursor from inicio)) -> 'cambios' -> 7
  @> '{"entidad": "evento", "fila": {"biologico": "Aftosa", "dosis": "2 ml", "via": "Subcutánea", "id_cria_resultante": null}}',
  'un Evento llega con los campos de su tipo');
select ok(
  not (pg_temp.pagina((select cursor from inicio)) -> 'cambios' -> 3 -> 'fila') ? 'cursor_descarga',
  'la fila no repite el cursor');
select ok(
  pg_temp.pagina((select cursor from inicio)) -> 'cambios' -> 6
  @> '{"entidad": "sibling", "fila": {"id_version": "5a000000-0000-0000-0000-000000000001", "resuelto_por": null}}',
  'trae el sibling sin resolver');

-- Páginas (RNF-09)
select ok(
  pg_temp.pagina((select cursor from inicio), 3) @> '{"hay_mas": true}'
  and jsonb_array_length(pg_temp.pagina((select cursor from inicio), 3) -> 'cambios') = 3,
  'una página trae a lo sumo el límite y dice si hay más');
select is(
  (pg_temp.pagina((select cursor from inicio), 3) ->> 'cursor')::bigint,
  (pg_temp.pagina((select cursor from inicio), 3) -> 'cambios' -> 2 ->> 'cursor')::bigint,
  'el cursor de la página es el de su último cambio');
select is(
  pg_temp.entidades(pg_temp.pagina((pg_temp.pagina((select cursor from inicio), 3) ->> 'cursor')::bigint)),
  array['animal', 'animal', 'chapeta', 'sibling', 'evento'],
  'la siguiente página sigue desde ese cursor');
select ok(
  pg_temp.pagina((pg_temp.pagina((select cursor from inicio)) ->> 'cursor')::bigint)
  @> '{"cambios": [], "referencias": [], "hay_mas": false}',
  'sin cambios nuevos no trae nada');
select is(
  (pg_temp.pagina((pg_temp.pagina((select cursor from inicio)) ->> 'cursor')::bigint) ->> 'cursor')::bigint,
  (pg_temp.pagina((select cursor from inicio)) ->> 'cursor')::bigint,
  'sin cambios nuevos el cursor no cambia');

-- Referencias: lo que una página apunta, aunque se haya escrito antes
select ok(
  pg_temp.pagina((pg_temp.pagina((select cursor from inicio), 7) ->> 'cursor')::bigint) -> 'referencias'
  @> '[{"entidad": "animal", "fila": {"nombre": "Lunita"}}, {"entidad": "animal", "fila": {"nombre": "Lucero"}},
       {"entidad": "usuario", "fila": {"nombre": "Operario A"}}]',
  'una página con solo el Evento trae su Animal, la madre de ese Animal y el Usuario que lo registró');
select ok(
  not (pg_temp.pagina((select cursor from inicio)) -> 'referencias' @> '[{"entidad": "animal"}]'),
  'no repite como referencia una fila que ya trae la página');

-- Una baja escrita después aparece en la siguiente descarga
set local role postgres;
create temp table hasta as
  select (public.descargar_cambios((select cursor from inicio), 100) ->> 'cursor')::bigint as cursor;
grant select on hasta to authenticated;
update public.animal set fecha_baja = now(), rev = 2 where id_animal = 'a0000000-0000-0000-0000-000000000002';
update public.sibling set resuelto_por = '1a000000-0000-0000-0000-000000000009', fecha_resolucion = now()
  where id_version = '5a000000-0000-0000-0000-000000000001';
set local role authenticated;
select is(
  pg_temp.entidades(pg_temp.pagina((select cursor from hasta))),
  array['animal', 'sibling'],
  'una baja y un sibling resuelto vuelven a bajar');
select ok(
  pg_temp.pagina((select cursor from hasta)) -> 'cambios' -> 1
  @> '{"fila": {"resuelto_por": "1a000000-0000-0000-0000-000000000009"}}',
  'el sibling resuelto llega con su resolución');

-- Ganadero B
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000b1","role":"authenticated"}', true);
select is(
  pg_temp.entidades(pg_temp.pagina((select cursor from inicio))),
  array['perfil_finca', 'usuario', 'animal'],
  'otra finca solo recibe lo suyo');

select throws_ok($$select public.descargar_cambios(0, 0)$$, '22023', null, 'el límite tiene que ser positivo');

-- Sin sesión
set local role anon;
select throws_ok($$select public.descargar_cambios(0, 10)$$, '42501', null, 'sin sesión no se descarga nada');

select * from finish();
rollback;
