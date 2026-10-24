-- Descarga de los cambios de los demás celulares (CU-20 paso 7, RF-36, RNF-09, ADR-0005).
--
-- `descargar_cambios(desde, limite)` devuelve, en el orden en que se escribieron, las filas de la finca cuyo
-- cursor es mayor que `desde`: PerfilFinca, Usuarios, Animales, Chapetas, Eventos, Registros, Pesajes
-- pendientes y siblings, resueltos o no. Corre como el usuario (SECURITY INVOKER), así que las políticas RLS
-- dejan solo las filas de su finca.
--
-- Cada fila trae su versión actual, así que una fila vieja puede llegar después de otra que apunta a ella (un
-- Evento nuevo de un Animal editado después). Por eso la página trae también, como referencias, los Animales
-- (con sus padres y madres) y los Usuarios a los que apuntan sus filas, para que el celular pueda guardarlas.
--
-- El margen de solapamiento lo resta el celular al empezar cada descarga: si lo restara la función en cada
-- página, una página llena dentro del margen se repetiría sin avanzar.
--
-- Respuesta: {"cambios": [{"entidad", "cursor", "fila"}], "referencias": [{"entidad", "fila"}],
--             "cursor": el del último cambio, o `desde` si no hay, "hay_mas": boolean}

create function public.descargar_cambios(desde bigint, limite integer) returns jsonb
language plpgsql stable security invoker set search_path = '' as $$
declare
  respuesta jsonb;
begin
  if limite is null or limite < 1 then
    raise exception 'El límite tiene que ser positivo' using errcode = '22023';
  end if;

  with recursive filas as (
    select 'perfil_finca' as entidad, t.cursor_descarga as cursor, to_jsonb(t) - 'cursor_descarga' as fila
      from public.perfil_finca t where t.cursor_descarga > desde
    union all
    select 'usuario', t.cursor_descarga, to_jsonb(t) - 'cursor_descarga'
      from public.usuario t where t.cursor_descarga > desde
    union all
    select 'animal', t.cursor_descarga, to_jsonb(t) - 'cursor_descarga'
      from public.animal t where t.cursor_descarga > desde
    union all
    select 'chapeta', t.cursor_descarga, to_jsonb(t) - 'cursor_descarga'
      from public.chapeta t where t.cursor_descarga > desde
    union all
    select 'evento', t.cursor_descarga, to_jsonb(t) - 'cursor_descarga'
      from public.evento t where t.cursor_descarga > desde
    union all
    select 'registro', t.cursor_descarga, to_jsonb(t) - 'cursor_descarga'
      from public.registro t where t.cursor_descarga > desde
    union all
    select 'pesaje_pendiente', t.cursor_descarga, to_jsonb(t) - 'cursor_descarga'
      from public.pesaje_pendiente t where t.cursor_descarga > desde
    union all
    select 'sibling', t.cursor_descarga, to_jsonb(t) - 'cursor_descarga'
      from public.sibling t where t.cursor_descarga > desde
  ),
  -- Un cambio más que el límite, para saber si hay otra página.
  siguientes as (select * from filas order by cursor limit limite + 1),
  pagina as (select * from siguientes order by cursor limit limite),
  -- Los Animales a los que apunta la página, con sus padres y madres, y los Usuarios.
  animales(id) as (
    select (p.fila ->> 'id_animal')::uuid from pagina p where p.entidad in ('chapeta', 'evento', 'registro')
    union
    select (p.fila ->> c)::uuid from pagina p, unnest(array['id_padre', 'id_madre']) c
      where p.entidad = 'animal' and p.fila ->> c is not null
    union
    select padre from animales x join public.animal a on a.id_animal = x.id,
      unnest(array[a.id_padre, a.id_madre]) padre
      where padre is not null
  ),
  usuarios(id) as (
    select (p.fila ->> c)::uuid from pagina p, unnest(array['id_usuario', 'id_usuario_resolucion']) c
      where p.entidad in ('evento', 'registro', 'pesaje_pendiente') and p.fila ->> c is not null
  ),
  -- Sin las que ya trae la página.
  referencias as (
    select 'animal' as entidad, to_jsonb(a) - 'cursor_descarga' as fila from public.animal a
      where a.id_animal in (select id from animales) and a.id_animal::text not in (
        select p.fila ->> 'id_animal' from pagina p where p.entidad = 'animal')
    union all
    select 'usuario', to_jsonb(u) - 'cursor_descarga' from public.usuario u
      where u.id_usuario in (select id from usuarios) and u.id_usuario::text not in (
        select p.fila ->> 'id_usuario' from pagina p where p.entidad = 'usuario')
  )
  select jsonb_build_object(
    'cambios', coalesce(
      (select jsonb_agg(jsonb_build_object('entidad', p.entidad, 'cursor', p.cursor, 'fila', p.fila) order by p.cursor)
       from pagina p),
      '[]'),
    'referencias', coalesce((select jsonb_agg(jsonb_build_object('entidad', r.entidad, 'fila', r.fila)) from referencias r), '[]'),
    'cursor', coalesce((select max(p.cursor) from pagina p), desde),
    'hay_mas', (select count(*) from siguientes) > limite)
  into respuesta;
  return respuesta;
end;
$$;

-- Solo una sesión iniciada descarga.
revoke execute on function public.descargar_cambios(bigint, integer) from public, anon;
grant execute on function public.descargar_cambios(bigint, integer) to authenticated;
