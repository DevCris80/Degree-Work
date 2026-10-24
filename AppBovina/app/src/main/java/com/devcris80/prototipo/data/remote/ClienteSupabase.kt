package com.devcris80.prototipo.data.remote

import com.devcris80.prototipo.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp

/** El cliente del proyecto Supabase, con la URL y la clave pública de local.properties. */
fun crearClienteSupabase(sesion: SesionCifrada): SupabaseClient =
    createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
        httpEngine = OkHttp.create()
        install(Auth) {
            sessionManager = sesion
        }
        install(Postgrest)
    }
