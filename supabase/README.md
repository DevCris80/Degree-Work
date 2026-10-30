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

La cuenta de una finca nueva se crea desde la app, en Crear cuenta, y la de un Operario con un código de invitación (ver abajo). Un Operario de prueba también se puede armar a mano:

1. En Authentication → Users, crear el usuario con correo y contraseña, y copiar su id.
2. En el SQL Editor, que corre como `postgres` y no pasa por RLS, crear su `Usuario` con ese id en una finca que ya exista:

   ```sql
   insert into public.usuario (id_usuario, id_perfil_finca, nombre, rol, fecha_modificacion)
     values ('<id del usuario>', '<id de la finca>', 'Operario de prueba', 'Operario', now());
   ```

Desde la app no se puede hacer: `perfil_finca` y `usuario` no conceden INSERT a `authenticated`, y solo el alta de una cuenta nueva los escribe, por el trigger `crear_finca`.

Crear cuenta necesita que el proyecto no pida confirmar el correo (Authentication → Sign In / Providers → Email → Confirm email desactivado): la app lee la finca con la sesión que deja el registro.

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

## Crear cuenta

El celular registra la identidad con Supabase Auth (`POST /auth/v1/signup`) y manda como metadatos del registro (`data`) `{"nombre": "...", "nombre_finca": "...", "version_autorizacion": "0.1-provisional"}` (CU-01, RN-20), o con `"codigo_invitacion": "..."` en lugar de `nombre_finca` (CU-04, ver Invitaciones). El trigger `crear_finca` de `auth.users` llama `privado.crear_finca` en la misma transacción del alta: crea la finca, su primer `Usuario` con rol Ganadero y una fila en `aceptacion_datos` con la versión del texto de la Ley 1581 y su fecha (RF-04, RNF-13). Si algo falla, Supabase Auth no da de alta la identidad. Después el celular lee su finca y su Usuario como al iniciar sesión.

- La versión tiene que estar en `privado.texto_autorizacion`; sin ella, o sin el nombre de la persona o de la finca, el alta falla con `22023`. También con `22023` falla un alta que trae `nombre_finca` y `codigo_invitacion` a la vez, o un código que no existe, ya se usó o venció. Cambiar el texto de la app es una migración que agrega su versión.
- Los metadatos no deciden el rol ni la finca: el primer Usuario siempre es Ganadero de una finca nueva. Un alta sin `nombre_finca` (un Operario creado a mano) no crea finca.
- `aceptacion_datos`, `texto_autorizacion` e `invitacion` solo existen en el servidor: no se leen ni se escriben por la API, y no viajan en la descarga. `privado.crear_finca` no se puede llamar por la API.

## Invitaciones

Un Operario entra a una finca con un código que genera el Ganadero (CU-04, RF-05, RN-18). Las dos piezas están en `20261029120000_invitaciones.sql`:

- `public.generar_invitacion()` (`POST /rest/v1/rpc/generar_invitacion`, sin argumentos, con el JWT del usuario) devuelve `{"codigo": "...", "fecha_vencimiento": "..."}`: ocho caracteres sin los confusos (sin 0, O, 1, I ni L), de un solo uso y con 7 días de vigencia. Es `SECURITY DEFINER` y comprueba el rol: un Operario, alguien sin finca o una llamada sin sesión reciben `42501`.
- `privado.canjear_invitacion` la llama el trigger de `auth.users` cuando el alta trae `codigo_invitacion` (ver Crear cuenta). Ahí la persona todavía no tiene sesión, por eso el canje va dentro del alta y no es una función pública. Bloquea la fila del código, que tiene que estar sin usar y vigente (acepta minúsculas y espacios alrededor); crea el `Usuario` como Operario de la finca del código, guarda la aceptación del texto de la Ley 1581 y marca el código como canjeado. Si algo falla (`22023`), Supabase Auth no da de alta la identidad y el código sigue sin usar.
- Los metadatos no deciden el rol ni la finca: solo el código. La tabla `invitacion` no se lee ni se escribe por la API, y `privado.canjear_invitacion` no se puede llamar por la API.

## Quitar a un Operario

El Ganadero revoca el acceso de un Operario a la finca (CU-05, RF-37, LIM-06). Está en `20261030120000_quitar_operario.sql`. La fila de `usuario` no se borra: Evento, Registro y Pesaje pendiente la referencian y nadie borra (RNF-04), así que sus registros anteriores siguen a su nombre. Se marca con `usuario.fecha_quitado`.

- `privado.mi_finca()` y `privado.mi_rol()` devuelven nulo para un Usuario quitado. Todas las políticas RLS dependen de ellas: desde ese momento no lee ni escribe nada de la finca ni genera códigos de invitación, y `subir_cambios` rechaza sus cambios. La cuenta de Supabase Auth sigue existiendo.
- `public.quitar_operario(id_operario uuid)` (`POST /rest/v1/rpc/quitar_operario`, con el JWT del usuario) es `SECURITY DEFINER`. Marca `fecha_quitado` y cambia `fecha_modificacion`, así que el cursor de descarga avanza. Devuelve `42501` si no hay sesión o quien llama no es Ganadero, y `22023` si el id no es de un Operario de su finca (otra finca, un Ganadero, alguien que no existe o que ya estaba quitado).
- `public.sigo_en_la_finca()` (`POST /rest/v1/rpc/sigo_en_la_finca`, sin argumentos) devuelve si la cuenta de la sesión sigue en una finca. Responde aunque la cuenta ya no pase por RLS. El celular lo llama al empezar cada sincronización: si dice falso, no sube ni baja nada y cierra la sesión.
- La lista de Operarios que ve el Ganadero se lee directo de `usuario`, que RLS limita a su finca. Trae también a los quitados, con `fecha_quitado`; la app los deja fuera.

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

## Descargar cambios

El celular llama `POST /rest/v1/rpc/descargar_cambios` con `{"desde": <cursor>, "limite": <tamaño de página>}`. La respuesta trae las filas de su finca con cursor mayor que `desde`, en el orden en que se escribieron:

```json
{"cambios": [{"entidad": "animal", "cursor": 812, "fila": {"id_animal": "<uuid>", "rev": 3, "...": "..."}}],
 "referencias": [{"entidad": "usuario", "fila": {"id_usuario": "<uuid>", "...": "..."}}],
 "cursor": 812, "hay_mas": true}
```

- `entidad` es `perfil_finca`, `usuario`, `animal`, `chapeta`, `evento`, `registro`, `pesaje_pendiente` o `sibling`. Los siblings llegan resueltos (`resuelto_por` no nulo) y sin resolver.
- `referencias` son los Animales (con sus padres y madres) y los Usuarios a los que apuntan los cambios de la página y que la página no trae. Una fila llega con su versión actual, así que un Evento nuevo puede llegar antes que su Animal editado después.
- `cursor` es el del último cambio, o `desde` si la página está vacía. La siguiente página se pide desde ahí.
- El margen de solapamiento lo resta el celular a su cursor guardado al empezar cada descarga, no en cada página: dos transacciones pueden confirmar en orden distinto al de sus cursores.

Un `limite` menor que 1 da `22023`.

## Fotos de los Animales

Las fotos viven en el bucket privado `fotos` de Storage, una por Animal, en `<id_perfil_finca>/<id_animal>.webp`: cada celular calcula la ruta sin preguntarle al servidor, y cambiar la foto reemplaza el archivo (`x-upsert: true`). No viajan en `subir_cambios` ni en `descargar_cambios` (RF-38).

- Solo cabe la foto de un Animal que ya está en el servidor y es de la finca del usuario; los dos roles la suben. Otra ruta se rechaza con `statusCode` `403` (`AccessDenied`), y una foto de otra finca no se ve: bajarla da `404`.
- La API de Storage rechaza con `415` un archivo que no es `image/webp`, y uno de más de 2 MiB. El celular sube WebP con el lado mayor de 1280 px como máximo y calidad 75 (RNF-10).
- Storage responde sus errores con HTTP 400 y el código en el `statusCode` del cuerpo.
- El celular reconoce estos rechazos por el `message` del cuerpo, porque supabase-kt no le pasa el `statusCode`: "new row violates row-level security policy" (403), "The object exceeded the maximum allowed size" (413) y "mime type … is not supported" (415). Reenviar la misma foto no los cambia, así que la foto queda en la bandeja como un Rechazo de entidad `foto` y no se reintenta. Un `404` (`Bucket not found`) o una sesión vencida sí se reintentan.
- Para saber qué fotos cambiaron, el celular lista la carpeta de su finca y compara el `updated_at` de cada archivo con el de la foto que tiene.
- Nadie borra (RNF-04): no hay política de DELETE, así que ni la API de Storage borra una foto.
