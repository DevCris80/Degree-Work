-- Las fotos de los Animales (RF-38, RNF-10, RNF-12).
--
-- Un bucket privado con una carpeta por finca y un archivo por Animal: `<id_perfil_finca>/<id_animal>.webp`. Cada
-- celular calcula la ruta sin preguntarle al servidor, y cambiar la foto reemplaza el archivo. La API de Storage
-- rechaza otro tipo de archivo o uno más grande que el límite; el celular sube WebP de 1280 px como máximo en su
-- lado mayor, que pesan bastante menos.
--
-- Las políticas son por finca, para los dos roles: el Operario también registra Animales con su foto. Nadie borra
-- (RNF-04): no hay política de DELETE, así que ni la API de Storage borra una foto.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
  values ('fotos', 'fotos', false, 2097152, array['image/webp']);

-- Si `ruta` es la de la foto de un Animal de la finca del usuario. Leer `animal` pasa por su RLS.
create function privado.es_foto_de_mi_finca(ruta text) returns boolean
language sql stable set search_path = '' as $$
  select exists (
    select 1 from public.animal a
    where a.id_perfil_finca = privado.mi_finca()
      and ruta = a.id_perfil_finca::text || '/' || a.id_animal::text || '.webp'
  )
$$;

create policy fotos_select on storage.objects for select to authenticated
  using (bucket_id = 'fotos' and (storage.foldername(name))[1] = privado.mi_finca()::text);

create policy fotos_insert on storage.objects for insert to authenticated
  with check (bucket_id = 'fotos' and privado.es_foto_de_mi_finca(name));

create policy fotos_update on storage.objects for update to authenticated
  using (bucket_id = 'fotos' and (storage.foldername(name))[1] = privado.mi_finca()::text)
  with check (bucket_id = 'fotos' and privado.es_foto_de_mi_finca(name));
