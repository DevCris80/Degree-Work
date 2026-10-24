# Supabase

Esquema central de MiGanado: las tablas de cada finca, sus políticas RLS por finca y rol, y las reglas que el servidor hace cumplir aunque un cliente se salte la app. Las decisiones están en `docs/adr/0004-…` (revisiones y siblings) y `docs/adr/0005-…` (sincronización con funciones de Postgres).

- `migrations/`: el esquema, en orden. Cada cambio es una migración nueva; una migración ya aplicada no se edita.
- `tests/database/`: pruebas pgTAP. Cada regla del esquema tiene la suya.
- `config.toml`: configuración del entorno local. No tiene secretos.

## Probar en local

Hace falta Docker o Podman. Con Podman, el CLI de Supabase necesita el socket:

```sh
systemctl --user start podman.socket
export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock
```

Desde la raíz del repositorio:

```sh
npx supabase@2.122.0 db start   # solo Postgres; la primera vez descarga la imagen
npx supabase@2.122.0 db reset   # borra la base local y aplica todas las migraciones desde cero
npx supabase@2.122.0 test db    # corre las pruebas pgTAP
npx supabase@2.122.0 stop
```

Las pruebas escriben sus datos dentro de una transacción que se deshace al terminar. Para actuar como un usuario, fijan los claims del JWT y cambian de rol:

```sql
select set_config('request.jwt.claims', '{"sub":"<id del usuario>","role":"authenticated"}', true);
set local role authenticated;
```

Una prueba que escribe directo en `animal`, `evento`, `registro`, `revision` o `sibling` como `authenticated` fija además la marca de `subir_cambios` (ver abajo): `select set_config('miganado.subir_cambios', 'si', true);`.

## El proyecto en la nube

Lo crea una sola vez el dueño del proyecto:

1. Crear el proyecto en [supabase.com](https://supabase.com) (región São Paulo, la más cercana).
2. `npx supabase@2.122.0 login` y `npx supabase@2.122.0 link --project-ref <ref del proyecto>`.
3. `npx supabase@2.122.0 db push --linked` aplica las migraciones pendientes. Se repite con cada migración nueva que llegue a `main`. Desde ese momento, esas migraciones ya no se editan: un cambio es una migración nueva.
4. `npx supabase@2.122.0 test db --linked` corre las mismas pruebas pgTAP contra la nube. Cada archivo trabaja dentro de una transacción que se deshace, así que no deja datos.

Mientras no exista Crear cuenta con Supabase (#87), una cuenta de prueba se arma a mano:

1. En Authentication → Users, crear el usuario con correo y contraseña, y copiar su id.
2. En el SQL Editor, que corre como `postgres` y no pasa por RLS, crear primero su finca y después su `Usuario` con ese id:

   ```sql
   insert into public.perfil_finca (id_perfil_finca, nombre_finca, fecha_modificacion)
     values (gen_random_uuid(), 'Finca de prueba', now()) returning id_perfil_finca;
   insert into public.usuario (id_usuario, id_perfil_finca, nombre, rol, fecha_modificacion)
     values ('<id del usuario>', '<id de la finca>', 'Ganadero de prueba', 'Ganadero', now());
   ```

Desde la app no se puede hacer: `perfil_finca` y `usuario` no conceden INSERT a `authenticated`.

## Credenciales para la app

La app solo usa dos valores públicos: la URL del proyecto y su clave pública (`anon` o *publishable*). Están en el panel de Supabase, en Project Settings → API. La clave `service_role` (*secret*) nunca va en la app ni en el repositorio.

Cada colaborador las pone en `AppBovina/local.properties`, que git ignora:

```properties
supabase.url=https://<ref del proyecto>.supabase.co
supabase.anonKey=<clave pública>
```

Para verlas, el dueño invita al colaborador a la organización de Supabase, o se las pasa por un canal privado.

## Lo que el esquema hace cumplir

Cada tabla replica una entidad de Room, con lo ya decidido para ella (padre y madre, ubicación de la finca, `rev`). En el servidor:
- No tiene `sincronizado`, `rev_base` ni `id_version`.
- Las fechas son `timestamptz`, y la hora de medición se llama `fecha_medicion`.
- Cada escritura toma un `cursor_descarga` nuevo de una secuencia global.

Evento, Registro, Chapeta y Pesaje pendiente llevan además `id_perfil_finca`, que llena un trigger a partir de su Animal o de su Usuario.

Nadie borra (RNF-04). Una tabla nueva no concede DELETE porque la migración de políticas lo quita de los permisos por defecto; los demás permisos (SELECT, INSERT, UPDATE) sí los da Supabase por defecto, así que cada migración que cree una tabla revoca lo que esa tabla no necesita y activa RLS.

Un usuario no escribe `animal`, `evento`, `registro`, `revision` ni `sibling` por fuera de `subir_cambios`: la función marca su transacción con `miganado.subir_cambios` y un trigger rechaza con `42501` la escritura de `authenticated` o `anon` que no la lleve. Así nadie salta la comparación de `rev_base` con un `PATCH` directo.

Errores que devuelve, para clasificar un rechazo:

| Código | Cuándo |
|---|---|
| `42501` | Fuera de la finca del usuario (RLS), una acción que su rol no permite, una fila a nombre de otro Usuario (RN-19), o un permiso de tabla que no existe (borrar, escribir `usuario`). |
| `23505` | Unicidad: una segunda Chapeta activa con el mismo código en la finca (RN-01), o un `id_lectura` repetido en la finca (RN-09, RN-21). |
| `23503` | Una referencia a un Animal o un Usuario de otra finca, o un Pesaje pendiente asignado sin su Registro (se comprueba al terminar la transacción). |
| `23514` | Datos inválidos: etapa que no corresponde al sexo, código sin normalizar, peso fuera de 0–1500 kg. |
| `22xxx` | Datos que no se pueden leer: un UUID o una fecha mal escritos, o una entidad que no se sube (`22023`). Solo los devuelve `subir_cambios`. |
| `55000` | Un estado definitivo: deshacer una baja, una Chapeta liberada, una conciliación o un sibling resuelto; o cambiar lo que no se edita (el código de una Chapeta, el peso de un Pesaje pendiente, la finca de un Animal). |

## Subir cambios

El celular llama `POST /rest/v1/rpc/subir_cambios` con `{"lote": [...]}` y el JWT del usuario (ADR-0005). Cada cambio del lote:

```json
{"entidad": "animal", "id_version": "<uuid>", "rev_base": 3, "resuelve": ["<id_version de un sibling>"],
 "fila": {"id_animal": "<uuid>", "nombre": "Lucero", "...": "las columnas de la tabla, en snake_case"}}
```

`id_version`, `rev_base` y `resuelve` van solo en Animal, Evento y Registro; `rev_base` es nulo en una fila que nunca subió. Las tablas hijas pueden enviar `id_perfil_finca` nulo: lo llena el trigger. De PerfilFinca solo cambian las columnas que trae la fila.

La respuesta trae un resultado por cambio, en el mismo orden:

| `resultado` | Cuándo | Trae |
|---|---|---|
| `aceptada` | `rev_base` es la vigente, o la fila es nueva, o es una baja (RN-22), o una Chapeta o un Pesaje pendiente que cambian de estado. | `rev` (si tiene revisiones) y `fila`, como quedó en el servidor. |
| `sibling` | `rev_base` ya no es la vigente (RN-15). Una baja que además cambió otros campos se aplica y el resto queda como sibling. | `rev` y `fila`: la revisión vigente. |
| `rechazada` | Uno de los errores de la tabla de arriba. El resto del lote sigue. | `codigo`, `motivo` y `fila`: la fila del servidor, o nula si el servidor no la tiene. |

Reenviar un `id_version` ya procesado devuelve el mismo resultado, sin crear revisión ni sibling (RN-23). El celular sube los Registros antes que los Pesajes pendientes: uno asignado sin su Registro se rechaza con `23503`.
