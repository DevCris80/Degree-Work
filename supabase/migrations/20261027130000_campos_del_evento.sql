-- Los campos de cada tipo de Evento (#128, RF-29): la vacunación guarda biológico, dosis y vía; el parto, la cría
-- resultante.
--
-- Son columnas de `evento`, así que viajan con su revisión: `subir_cambios` las escribe con
-- `columnas_editables` y `descargar_cambios` las trae con el resto de la fila, sin tocar ninguna función. Todas
-- son opcionales: los Eventos que ya existían y los de otros tipos las dejan vacías.
--
-- `id_cria_resultante` no referencia a `animal`: el Evento puede subir antes que su cría, y la referencia no
-- protege nada, porque apunta a un Animal que el celular ya conoce.
alter table public.evento
  add column biologico text,
  add column dosis text,
  add column via text,
  add column id_cria_resultante uuid;
