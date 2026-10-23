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

## El proyecto en la nube

Lo crea una sola vez el dueño del proyecto:

1. Crear el proyecto en [supabase.com](https://supabase.com) (región São Paulo, la más cercana).
2. `npx supabase@2.122.0 login` y `npx supabase@2.122.0 link --project-ref <ref del proyecto>`.
3. `npx supabase@2.122.0 db push` aplica las migraciones pendientes. Se repite con cada migración nueva que llegue a `main`.

## Credenciales para la app

La app solo usa dos valores públicos: la URL del proyecto y su clave pública (`anon` o *publishable*). Están en el panel de Supabase, en Project Settings → API. La clave `service_role` (*secret*) nunca va en la app ni en el repositorio.

Cada colaborador las pone en `AppBovina/local.properties`, que git ignora:

```properties
supabase.url=https://<ref del proyecto>.supabase.co
supabase.anonKey=<clave pública>
```

Para verlas, el dueño invita al colaborador a la organización de Supabase, o se las pasa por un canal privado.

## Lo que el esquema hace cumplir

Cada tabla replica una entidad de Room, salvo por tres cosas:
- No tiene `sincronizado`, `rev_base` ni `id_version`.
- Las fechas son `timestamptz`.
- Cada escritura toma un `cursor_descarga` nuevo de una secuencia global.

Evento, Registro, Chapeta y Pesaje pendiente llevan además `id_perfil_finca`, que llena un trigger a partir de su Animal o de su Usuario.

Errores que devuelve, para clasificar un rechazo:

| Código | Cuándo |
|---|---|
| `42501` | Fuera de la finca del usuario (RLS), una acción que su rol no permite, o un permiso de tabla que no existe (borrar, escribir `usuario`). |
| `23505` | Unicidad: una segunda Chapeta activa con el mismo código en la finca (RN-01), o un `id_lectura` repetido (RN-09, RN-21). |
| `23503` | Una referencia a un Animal o un Usuario de otra finca. |
| `23514` | Datos inválidos: etapa que no corresponde al sexo, código sin normalizar, peso fuera de 0–1500 kg. |
| `55000` | Un estado definitivo: deshacer una baja, una Chapeta liberada, una conciliación o un sibling resuelto; o cambiar lo que no se edita (el código de una Chapeta, el peso de un Pesaje pendiente, la finca de un Animal). |
