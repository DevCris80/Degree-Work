-- subir_cambios (CU-20, ADR-0004, ADR-0005): revisiones, siblings, bajas, reenvíos, resoluciones y rechazos.
begin;
-- En la nube el CLI entra con un rol temporal; las pruebas corren como postgres, igual que en local.
set local role postgres;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions;
select plan(34);

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-0000000000a1', 'ganadero.a@test'),
  ('00000000-0000-0000-0000-0000000000a2', 'operario.a@test');
insert into public.perfil_finca (id_perfil_finca, nombre_finca, latitud, longitud, fecha_modificacion) values
  ('0000000a-0000-0000-0000-000000000000', 'Finca A', 4.6, -74.1, now());
insert into public.usuario (id_usuario, id_perfil_finca, nombre, rol, fecha_modificacion) values
  ('00000000-0000-0000-0000-0000000000a1', '0000000a-0000-0000-0000-000000000000', 'Ganadero A', 'Ganadero', now()),
  ('00000000-0000-0000-0000-0000000000a2', '0000000a-0000-0000-0000-000000000000', 'Operario A', 'Operario', now());

-- Un cambio del Animal Lucero, tal como lo arma el celular.
create function pg_temp.lucero(id_version text, rev_base int, raza text, fecha_baja text default null,
  resuelve jsonb default null) returns jsonb
language sql as $$
  select jsonb_build_object('entidad', 'animal', 'id_version', id_version, 'rev_base', rev_base, 'resuelve', resuelve,
    'fila', jsonb_build_object(
      'id_animal', 'a0000000-0000-0000-0000-000000000001', 'id_perfil_finca', '0000000a-0000-0000-0000-000000000000',
      'id_padre', null, 'id_madre', null, 'nombre', 'Lucero', 'raza', raza, 'sexo', 'Hembra', 'etapa', 'Vaca',
      'fecha_nacimiento', '2024-01-01T00:00:00Z', 'fecha_nacimiento_es_estimada', false, 'proposito', 'Leche',
      'fecha_baja', fecha_baja, 'fecha_modificacion', '2026-10-23T10:00:00Z'))
$$;

-- Sube un solo cambio y devuelve su resultado.
create function pg_temp.subir(cambio jsonb) returns jsonb
language sql as $$ select public.subir_cambios(jsonb_build_array(cambio)) -> 0 $$;

-- Ganadero
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}', true);
set local role authenticated;

-- Revisiones
select ok(
  pg_temp.subir(pg_temp.lucero('1a000000-0000-0000-0000-000000000001', null, 'Brahman'))
  @> '{"resultado": "aceptada", "rev": 1, "id_fila": "a0000000-0000-0000-0000-000000000001"}',
  'un Animal nuevo se acepta como revisión 1');
select ok(
  pg_temp.subir(pg_temp.lucero('1a000000-0000-0000-0000-000000000002', 1, 'Brahman rojo'))
  @> '{"resultado": "aceptada", "rev": 2, "fila": {"raza": "Brahman rojo", "rev": 2}}',
  'una edición desde la revisión vigente se acepta como la siguiente');
select ok(
  pg_temp.subir(pg_temp.lucero('1a000000-0000-0000-0000-000000000003', 1, 'Gyr'))
  @> '{"resultado": "sibling", "rev": 2, "fila": {"raza": "Brahman rojo"}}',
  'una edición desde una revisión vieja queda como sibling y recibe la vigente');
select is(
  (select count(*)::int from public.revision where id_fila = 'a0000000-0000-0000-0000-000000000001'), 2,
  'el sibling no crea revisión');

-- Reenvíos (RN-23)
select ok(
  pg_temp.subir(pg_temp.lucero('1a000000-0000-0000-0000-000000000002', 1, 'Brahman rojo'))
  @> '{"resultado": "aceptada", "rev": 2}',
  'reenviar una edición aceptada recibe la misma respuesta');
select ok(
  pg_temp.subir(pg_temp.lucero('1a000000-0000-0000-0000-000000000003', 1, 'Gyr'))
  @> '{"resultado": "sibling", "rev": 2}',
  'reenviar un sibling recibe la misma respuesta');
select results_eq(
  $$select (select count(*)::int from public.revision), (select count(*)::int from public.sibling)$$,
  $$values (2, 1)$$,
  'reenviar no crea revisiones ni siblings');

-- Bajas (RN-22)
select ok(
  pg_temp.subir(
    pg_temp.lucero('1a000000-0000-0000-0000-000000000004', 1, 'Brahman', fecha_baja => '2026-10-23T11:00:00Z'))
  @> '{"resultado": "aceptada", "rev": 3, "fila": {"raza": "Brahman rojo", "fecha_baja": "2026-10-23T11:00:00+00:00"}}',
  'una baja desde una revisión vieja se aplica sobre la vigente');
select is(
  (select count(*)::int from public.sibling where id_version = '1a000000-0000-0000-0000-000000000004'), 0,
  'la baja no queda como sibling');

-- Un Animal dado de baja y editado desde la revisión 1 al mismo tiempo.
select pg_temp.subir(jsonb_set(pg_temp.lucero('1b000000-0000-0000-0000-000000000001', null, 'Cebú'),
  '{fila,id_animal}', '"a0000000-0000-0000-0000-000000000002"'));
select pg_temp.subir(jsonb_set(pg_temp.lucero('1b000000-0000-0000-0000-000000000002', 1, 'Cebú rojo'),
  '{fila,id_animal}', '"a0000000-0000-0000-0000-000000000002"'));
select ok(
  pg_temp.subir(jsonb_set(
    pg_temp.lucero('1b000000-0000-0000-0000-000000000003', 1, 'Nelore', fecha_baja => '2026-10-23T11:00:00Z'),
    '{fila,id_animal}', '"a0000000-0000-0000-0000-000000000002"'))
  @> '{"resultado": "sibling", "rev": 3, "fila": {"raza": "Cebú rojo", "fecha_baja": "2026-10-23T11:00:00+00:00"}}',
  'una baja que además cambia otros campos se aplica, y lo demás queda como sibling');
select ok(
  pg_temp.subir(jsonb_set(
    pg_temp.lucero('1b000000-0000-0000-0000-000000000004', 1, 'Cebú', fecha_baja => '2026-10-23T12:00:00Z'),
    '{fila,id_animal}', '"a0000000-0000-0000-0000-000000000002"'))
  @> '{"resultado": "aceptada", "rev": 3, "fila": {"fecha_baja": "2026-10-23T11:00:00+00:00"}}',
  'una baja de una fila que otro celular ya dio de baja se acepta sin cambiar la baja');
select is(
  (select count(*)::int from public.sibling where id_version = '1b000000-0000-0000-0000-000000000004'), 0,
  'la segunda baja no queda como sibling');

-- Un Animal subido cuya respuesta no llegó: el celular lo edita sin conocer su revisión.
select pg_temp.subir(jsonb_set(pg_temp.lucero('1c000000-0000-0000-0000-000000000001', null, 'Gyr'),
  '{fila,id_animal}', '"a0000000-0000-0000-0000-000000000003"'));
select ok(
  pg_temp.subir(jsonb_set(pg_temp.lucero('1c000000-0000-0000-0000-000000000002', null, 'Gyr lechero'),
    '{fila,id_animal}', '"a0000000-0000-0000-0000-000000000003"'))
  @> '{"resultado": "aceptada", "rev": 2}',
  'una edición sin rev_base de una fila que ya está en el servidor parte de la revisión 1');

-- Resolución de un Conflicto (RN-17): dos siblings del mismo Animal y una resolución que nombra solo uno.
create function pg_temp.mariposa(id_version text, rev_base int, raza text, resuelve jsonb default null) returns jsonb
language sql as $$
  select jsonb_set(pg_temp.lucero(id_version, rev_base, raza, resuelve => resuelve),
    '{fila,id_animal}', '"a0000000-0000-0000-0000-000000000003"')
$$;
select pg_temp.subir(pg_temp.mariposa('1c000000-0000-0000-0000-000000000003', 1, 'Holstein'));
select pg_temp.subir(pg_temp.mariposa('1c000000-0000-0000-0000-000000000004', 1, 'Jersey'));
select ok(
  pg_temp.subir(pg_temp.mariposa('1c000000-0000-0000-0000-000000000005', 1, 'Pardo',
    resuelve => '["1c000000-0000-0000-0000-000000000004"]'))
  @> '{"resultado": "sibling"}',
  'una resolución desde una revisión vieja queda como sibling');
select ok(
  pg_temp.subir(pg_temp.mariposa('1c000000-0000-0000-0000-000000000006', 2, 'Holstein',
    resuelve => '["1c000000-0000-0000-0000-000000000003"]'))
  @> '{"resultado": "aceptada", "rev": 3}',
  'una resolución desde la vigente se acepta');
select results_eq(
  $$select id_version::text, resuelto_por::text from public.sibling
    where id_fila = 'a0000000-0000-0000-0000-000000000003' order by id_version$$,
  $$values ('1c000000-0000-0000-0000-000000000003', '1c000000-0000-0000-0000-000000000006'),
    ('1c000000-0000-0000-0000-000000000004', null), ('1c000000-0000-0000-0000-000000000005', null)$$,
  'la resolución cierra solo los siblings que nombra');

-- Chapeta y Pesaje pendiente: se insertan por UUID y cambian de estado una vez.
create function pg_temp.chapeta(id_chapeta text, id_animal text, fecha_desasociacion text default null) returns jsonb
language sql as $$
  select jsonb_build_object('entidad', 'chapeta', 'fila', jsonb_build_object(
    'id_chapeta', id_chapeta, 'codigo', '04A31B2C', 'id_animal', id_animal, 'id_perfil_finca', null,
    'fecha_asociacion', '2026-10-23T09:00:00Z', 'fecha_desasociacion', fecha_desasociacion,
    'fecha_modificacion', '2026-10-23T10:00:00Z'))
$$;
create function pg_temp.pendiente(resolucion text, fecha_resolucion text) returns jsonb
language sql as $$
  select jsonb_build_object('entidad', 'pesaje_pendiente', 'fila', jsonb_build_object(
    'id_pesaje_pendiente', 'd0000000-0000-0000-0000-000000000001', 'id_lectura', 'lectura-1', 'codigo', '0BADC0DE',
    'peso', 380.5, 'fecha_medicion', '2026-10-23T09:00:00Z', 'id_usuario', '00000000-0000-0000-0000-0000000000a1',
    'id_perfil_finca', null, 'motivo', 'CODIGO_SIN_CHAPETA_ACTIVA', 'resolucion', resolucion,
    'id_usuario_resolucion', case when resolucion is not null then '00000000-0000-0000-0000-0000000000a1' end,
    'fecha_resolucion', fecha_resolucion, 'fecha_modificacion', '2026-10-23T10:00:00Z'))
$$;

select ok(
  pg_temp.subir(pg_temp.chapeta('c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000003'))
  @> '{"resultado": "aceptada", "id_fila": "c0000000-0000-0000-0000-000000000001"}',
  'una Chapeta nueva se acepta');
select ok(
  public.subir_cambios(jsonb_build_array(
    pg_temp.chapeta('c0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001'),
    jsonb_build_object('entidad', 'evento', 'id_version', '1e000000-0000-0000-0000-000000000001', 'rev_base', null,
      'fila', jsonb_build_object('id_evento', 'e0000000-0000-0000-0000-000000000001',
        'id_animal', 'a0000000-0000-0000-0000-000000000003', 'id_usuario', '00000000-0000-0000-0000-0000000000a1',
        'id_perfil_finca', null, 'tipo_evento', 'Vacunación', 'fecha', '2026-10-23T09:00:00Z', 'detalle', 'Aftosa',
        'fecha_baja', null, 'fecha_modificacion', '2026-10-23T10:00:00Z'))))
  @> '[{"resultado": "rechazada", "codigo": "23505", "fila": null}, {"resultado": "aceptada", "rev": 1}]',
  'una Chapeta con un código que ya tiene Chapeta activa se rechaza, y el resto del lote se acepta');
select ok(
  pg_temp.subir(pg_temp.chapeta('c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000003',
    '2026-10-23T12:00:00Z'))
  @> '{"resultado": "aceptada", "fila": {"fecha_desasociacion": "2026-10-23T12:00:00+00:00"}}',
  'liberar una Chapeta subida se acepta');
select ok(
  pg_temp.subir(pg_temp.chapeta('c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000003',
    '2026-10-23T12:00:00Z'))
  @> '{"resultado": "aceptada"}',
  'reenviar la liberación recibe la misma respuesta');
select pg_temp.subir(pg_temp.pendiente(null, null));
select pg_temp.subir(pg_temp.pendiente('DESCARTADO', '2026-10-23T12:00:00Z'));
select ok(
  pg_temp.subir(pg_temp.pendiente('DESCARTADO', '2026-10-23T13:00:00Z'))
  @> '{"resultado": "rechazada", "codigo": "55000", "fila": {"fecha_resolucion": "2026-10-23T12:00:00+00:00"}}',
  'un Pesaje pendiente ya conciliado se rechaza y recibe la conciliación del servidor');
select ok(
  pg_temp.subir(jsonb_set(jsonb_set(pg_temp.pendiente('ASIGNADO', '2026-10-23T12:00:00Z'),
    '{fila,id_pesaje_pendiente}', '"d0000000-0000-0000-0000-000000000002"'), '{fila,id_lectura}', '"lectura-2"'))
  @> '{"resultado": "rechazada", "codigo": "23503", "fila": null}',
  'un Pesaje pendiente asignado sin su Registro se rechaza en su propio cambio');

-- Los campos de cada tipo de Evento (RF-29)
select ok(
  pg_temp.subir(jsonb_build_object('entidad', 'evento', 'id_version', '1e000000-0000-0000-0000-000000000002',
    'rev_base', null, 'fila', jsonb_build_object('id_evento', 'e0000000-0000-0000-0000-000000000002',
      'id_animal', 'a0000000-0000-0000-0000-000000000003', 'id_usuario', '00000000-0000-0000-0000-0000000000a1',
      'id_perfil_finca', null, 'tipo_evento', 'Parto', 'fecha', '2026-10-23T09:00:00Z', 'detalle', 'Sin complicaciones',
      'id_cria_resultante', 'a0000000-0000-0000-0000-000000000001', 'fecha_baja', null,
      'fecha_modificacion', '2026-10-23T10:00:00Z')))
  @> '{"resultado": "aceptada", "rev": 1, "fila": {"id_cria_resultante": "a0000000-0000-0000-0000-000000000001", "biologico": null}}',
  'un parto sube con su cría resultante');
select ok(
  pg_temp.subir(jsonb_build_object('entidad', 'evento', 'id_version', '1e000000-0000-0000-0000-000000000003',
    'rev_base', 1, 'fila', jsonb_build_object('id_evento', 'e0000000-0000-0000-0000-000000000001',
      'id_animal', 'a0000000-0000-0000-0000-000000000003', 'id_usuario', '00000000-0000-0000-0000-0000000000a1',
      'id_perfil_finca', null, 'tipo_evento', 'Vacunación', 'fecha', '2026-10-23T09:00:00Z', 'detalle', 'Aftosa',
      'biologico', 'Aftosa', 'dosis', '2 ml', 'via', 'Subcutánea', 'fecha_baja', null,
      'fecha_modificacion', '2026-10-23T11:00:00Z')))
  @> '{"resultado": "aceptada", "rev": 2, "fila": {"biologico": "Aftosa", "dosis": "2 ml", "via": "Subcutánea"}}',
  'editar una vacunación guarda el biológico, la dosis y la vía como la revisión siguiente');

select ok(
  pg_temp.subir('{"entidad": "usuario", "fila": {}}') @> '{"resultado": "rechazada", "codigo": "22023"}',
  'una entidad que no se sube se rechaza');

-- PerfilFinca
create function pg_temp.finca(nombre text) returns jsonb
language sql as $$
  select jsonb_build_object('entidad', 'perfil_finca', 'fila', jsonb_build_object(
    'id_perfil_finca', '0000000a-0000-0000-0000-000000000000', 'nombre_finca', nombre,
    'fecha_modificacion', '2026-10-23T10:00:00Z'))
$$;
select ok(
  pg_temp.subir(pg_temp.finca('La Esperanza'))
  @> '{"resultado": "aceptada", "fila": {"nombre_finca": "La Esperanza", "latitud": 4.6, "longitud": -74.1}}',
  'el Ganadero sube los datos de su finca sin borrar lo que el celular no envía');

-- Sin pasar por subir_cambios
select throws_ok(
  $$update public.animal set rev = 99 where id_animal = 'a0000000-0000-0000-0000-000000000001'$$,
  '42501', null, 'un Animal no se modifica sin pasar por subir_cambios');
select throws_ok(
  $$update public.evento set detalle = 'otro' where id_evento = 'e0000000-0000-0000-0000-000000000001'$$,
  '42501', null, 'un Evento no se modifica sin pasar por subir_cambios');
select throws_ok(
  $$insert into public.registro (id_registro, id_animal, id_usuario, peso, fecha_medicion, fecha_modificacion)
    values (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-0000000000a1', 300, now(), now())$$,
  '42501', null, 'un Registro no se escribe sin pasar por subir_cambios');
select throws_ok(
  $$insert into public.revision (id_version, entidad, id_fila, id_perfil_finca, rev, contenido)
    values (gen_random_uuid(), 'animal', gen_random_uuid(), '0000000a-0000-0000-0000-000000000000', 1, '{}')$$,
  '42501', null, 'una revisión no se escribe sin pasar por subir_cambios');
select throws_ok(
  $$update public.sibling set resuelto_por = gen_random_uuid(), fecha_resolucion = now()
    where id_version = '1c000000-0000-0000-0000-000000000004'$$,
  '42501', null, 'un sibling no se resuelve sin pasar por subir_cambios');

-- Operario
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a2","role":"authenticated"}', true);

select ok(
  pg_temp.subir(jsonb_set(pg_temp.mariposa('1d000000-0000-0000-0000-000000000001', 3, 'Holstein'),
    '{fila,fecha_baja}', '"2026-10-23T12:00:00Z"'))
  @> '{"resultado": "rechazada", "codigo": "42501", "fila": {"rev": 3, "fecha_baja": null}}',
  'un Operario que sube la baja de un Animal recibe un rechazo por permiso y la fila del servidor');
select ok(
  pg_temp.subir(pg_temp.finca('Otra'))
  @> '{"resultado": "rechazada", "codigo": "42501", "fila": {"nombre_finca": "La Esperanza"}}',
  'un Operario no sube los datos de la finca');

-- Sin sesión
set local role anon;
select throws_ok($$select public.subir_cambios('[]')$$, '42501', null, 'sin sesión no se sube nada');

select * from finish();
rollback;
