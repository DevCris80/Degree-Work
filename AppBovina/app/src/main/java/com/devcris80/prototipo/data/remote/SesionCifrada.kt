package com.devcris80.prototipo.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda la sesión de Supabase Auth cifrada con una clave AES del Android Keystore (RNF-11), nunca
 * en Room ni en texto plano. Si la clave ya no existe (por ejemplo, tras restaurar una copia de
 * seguridad en otro celular), la sesión guardada no se puede leer y se descarta: hay que volver a
 * iniciar sesión.
 */
class SesionCifrada(context: Context) : SessionManager {
    private val preferencias = context.getSharedPreferences("sesion_supabase", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _guardada = MutableStateFlow(preferencias.contains(CLAVE_SESION))
    val guardada: StateFlow<Boolean> = _guardada.asStateFlow()

    override suspend fun saveSession(session: UserSession) {
        preferencias.edit().putString(CLAVE_SESION, cifrar(json.encodeToString(UserSession.serializer(), session))).apply()
        _guardada.value = true
    }

    override suspend fun loadSession(): UserSession? {
        val cifrada = preferencias.getString(CLAVE_SESION, null) ?: return null
        return runCatching { json.decodeFromString(UserSession.serializer(), descifrar(cifrada)) }
            .onFailure { deleteSession() }
            .getOrNull()
    }

    override suspend fun deleteSession() {
        preferencias.edit().remove(CLAVE_SESION).apply()
        _guardada.value = false
    }

    private fun cifrar(texto: String): String {
        val cifrador = Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.ENCRYPT_MODE, clave()) }
        return Base64.encodeToString(cifrador.iv + cifrador.doFinal(texto.toByteArray()), Base64.NO_WRAP)
    }

    private fun descifrar(guardado: String): String {
        val bytes = Base64.decode(guardado, Base64.NO_WRAP)
        val cifrador = Cipher.getInstance(TRANSFORMACION).apply {
            init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(BITS_ETIQUETA, bytes, 0, BYTES_IV))
        }
        return String(cifrador.doFinal(bytes, BYTES_IV, bytes.size - BYTES_IV))
    }

    private fun clave(): SecretKey {
        val almacen = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val CLAVE_SESION = "sesion"
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "sesion_supabase"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val BYTES_IV = 12
        const val BITS_ETIQUETA = 128
    }
}
