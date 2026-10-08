---
status: accepted
---

# Supabase Auth como única identidad, sin autenticación local

Una finca tiene varios `Usuario`, cada uno con la app en su propio dispositivo y con un `rol`, y nadie sin permiso debe poder cambiar los datos. Decidimos que la identidad sea Supabase Auth desde el principio, sin la etapa intermedia de contraseña con hash en Room: un usuario creado localmente no existe en los demás dispositivos, y la restricción "quién puede cambiar qué" solo se garantiza en el servidor (reglas por finca y por rol sobre cada fila).

Offline-first se conserva porque la sesión y el `Usuario` con su `rol` quedan guardados en el dispositivo: después del primer inicio de sesión, todo funciona sin señal. El rol se verifica en dos niveles — en el dominio (funciona sin señal) y en Supabase (la barrera real).

## Consequences

- Se necesita señal para crear la cuenta, para iniciar sesión por primera vez en un dispositivo y para unirse a una finca. Es una excepción deliberada a offline-first.
- Un `Usuario` pertenece a una sola finca; alguien que trabaja en dos fincas usa dos cuentas.
- Un cambio de rol hecho mientras otro dispositivo está sin señal solo se hace efectivo cuando ese dispositivo sincroniza; el servidor puede rechazar escrituras hechas con un rol que ya no es válido.
