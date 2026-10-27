-- Las fotos de los Animales (RF-38, RNF-10, RNF-12): un bucket privado de WebP, una carpeta por finca, un archivo
-- por Animal, y nadie borra.
begin;
set local role postgres;
create extension if not exists pgtap with schema extensions;
set local search_path = public, extensions;
select set_config('miganado.subir_cambios', 'si', true);
select plan(15);

-- Finca A con su Ganadero, su Operario y dos Animales; finca B con su Ganadero y un Animal.
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
insert into public.animal (id_animal, id_perfil_finca, nombre, raza, sexo, etapa, fecha_nacimiento, proposito, fecha_modificacion) values
  ('a0000000-0000-0000-0000-000000000001', '0000000a-0000-0000-0000-000000000000', 'Lucero', 'Brahman', 'Hembra', 'Vaca', now(), 'Leche', now()),
  ('a0000000-0000-0000-0000-000000000002', '0000000a-0000-0000-0000-000000000000', 'Tormenta', 'Gyr', 'Hembra', 'Ternera', now(), 'Leche', now()),
  ('b0000000-0000-0000-0000-000000000001', '0000000b-0000-0000-0000-000000000000', 'Canela', 'Cebú', 'Hembra', 'Vaca', now(), 'Carne', now());

select results_eq(
  $$select public, file_size_limit, allowed_mime_types from storage.buckets where id = 'fotos'$$,
  $$values (false, 2097152::bigint, array['image/webp'])$$,
  'el bucket de fotos es privado, de WebP y de 2 MiB como máximo');

-- El Ganadero y el Operario de A suben la foto de un Animal de su finca.
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}', true);
set local role authenticated;
select lives_ok(
  $$insert into storage.objects (bucket_id, name) values ('fotos', '0000000a-0000-0000-0000-000000000000/a0000000-0000-0000-0000-000000000001.webp')$$,
  'el Ganadero sube la foto de un Animal de su finca');
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a2","role":"authenticated"}', true);
select lives_ok(
  $$insert into storage.objects (bucket_id, name) values ('fotos', '0000000a-0000-0000-0000-000000000000/a0000000-0000-0000-0000-000000000002.webp')$$,
  'el Operario también: registra Animales con su foto');

-- Solo cabe la foto de un Animal de la finca, en la carpeta de la finca.
select throws_ok(
  $$insert into storage.objects (bucket_id, name) values ('fotos', '0000000b-0000-0000-0000-000000000000/b0000000-0000-0000-0000-000000000001.webp')$$,
  '42501', null, 'no sube a la carpeta de otra finca');
select throws_ok(
  $$insert into storage.objects (bucket_id, name) values ('fotos', '0000000a-0000-0000-0000-000000000000/b0000000-0000-0000-0000-000000000001.webp')$$,
  '42501', null, 'ni la foto de un Animal de otra finca a la suya');
select throws_ok(
  $$insert into storage.objects (bucket_id, name) values ('fotos', '0000000a-0000-0000-0000-000000000000/otra.webp')$$,
  '42501', null, 'ni un archivo que no es la foto de un Animal');
select throws_ok(
  $$insert into storage.objects (bucket_id, name) values ('fotos', '0000000a-0000-0000-0000-000000000000/x/a0000000-0000-0000-0000-000000000001.webp')$$,
  '42501', null, 'ni en una subcarpeta');
select throws_ok(
  $$insert into storage.objects (bucket_id, name) values ('fotos', '0000000a-0000-0000-0000-000000000000/a0000000-0000-0000-0000-000000000001.jpg')$$,
  '42501', null, 'ni con otra extensión');

-- Cambiar la foto reemplaza el archivo del Animal.
select results_eq(
  $$with cambiada as (
      update storage.objects set metadata = '{"size": 1}'
      where bucket_id = 'fotos' and name = '0000000a-0000-0000-0000-000000000000/a0000000-0000-0000-0000-000000000001.webp'
      returning 1)
    select count(*)::int from cambiada$$,
  $$values (1)$$,
  'el Operario cambia la foto de un Animal de su finca');
select results_eq(
  $$select count(*)::int from storage.objects where bucket_id = 'fotos'$$, $$values (2)$$,
  'la finca A ve sus dos fotos');

-- La finca B no ve ni cambia las fotos de A.
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000b1","role":"authenticated"}', true);
select results_eq($$select count(*)::int from storage.objects where bucket_id = 'fotos'$$, $$values (0)$$,
  'la finca B no ve las fotos de A');
select results_eq(
  $$with cambiada as (update storage.objects set metadata = '{}' where bucket_id = 'fotos' returning 1)
    select count(*)::int from cambiada$$,
  $$values (0)$$,
  'ni las cambia');

-- Y A no se lleva una foto a la carpeta de B.
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}', true);
select throws_ok(
  $$update storage.objects set name = '0000000b-0000-0000-0000-000000000000/b0000000-0000-0000-0000-000000000001.webp'
    where bucket_id = 'fotos'$$,
  '42501', null, 'una foto no cambia de finca');

-- Sin sesión no se ve nada.
set local role anon;
select results_eq($$select count(*)::int from storage.objects where bucket_id = 'fotos'$$, $$values (0)$$,
  'sin sesión no se ve ninguna foto');

-- Nadie borra (RNF-04): ni siquiera por la API de Storage, que habilita el borrado antes de pasar por RLS.
select set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}', true);
set local role authenticated;
select set_config('storage.allow_delete_query', 'true', true);
delete from storage.objects where bucket_id = 'fotos';
set local role postgres;
select results_eq($$select count(*)::int from storage.objects where bucket_id = 'fotos'$$, $$values (2)$$,
  'el Ganadero no borra una foto');

select * from finish();
rollback;
