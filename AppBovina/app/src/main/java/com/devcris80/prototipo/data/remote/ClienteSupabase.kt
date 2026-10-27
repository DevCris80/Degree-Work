package com.devcris80.prototipo.data.remote

import com.devcris80.prototipo.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlin.time.Duration.Companion.seconds

/** El cliente del proyecto Supabase, con la URL y la clave pública de local.properties. */
fun crearClienteSupabase(sesion: SesionCifrada): SupabaseClient =
    createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
        httpEngine = OkHttp.create()
        // Corto, para que con señal débil una llamada colgada falle pronto y se reintente (CU-20 1b).
        requestTimeout = 10.seconds
        install(Auth) {
            sessionManager = sesion
        }
        install(Postgrest)
        install(Storage)
    }
