
package com.example.frutosapp.api

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64

import java.security.KeyStore

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SesionSegura(context: Context) {

    private val preferencias = context.applicationContext
        .getSharedPreferences(
            "natur_cosecha_sesion_segura",
            Context.MODE_PRIVATE
        )

    companion object {
        private const val ALIAS =
            "natur_cosecha_refresh_key"

        private const val CLAVE_REFRESH =
            "refresh_token_cifrado"

        private const val TRANSFORMACION =
            "AES/GCM/NoPadding"
    }

    private fun obtenerClave(): SecretKey {

        val keyStore = KeyStore.getInstance(
            "AndroidKeyStore"
        ).apply {
            load(null)
        }

        val existente = keyStore.getKey(
            ALIAS,
            null
        ) as? SecretKey

        if (existente != null) {
            return existente
        }

        val generador = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val parametros = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(
                KeyProperties.BLOCK_MODE_GCM
            )
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .setKeySize(256)
            .build()

        generador.init(parametros)

        return generador.generateKey()
    }

    @Synchronized
    fun guardarRefreshToken(token: String) {

        require(token.isNotBlank()) {
            "El refresh token no puede estar vacío."
        }

        val cipher = Cipher.getInstance(
            TRANSFORMACION
        )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            obtenerClave()
        )

        val cifrado = cipher.doFinal(
            token.toByteArray(Charsets.UTF_8)
        )

        val ivTexto = Base64.encodeToString(
            cipher.iv,
            Base64.NO_WRAP
        )

        val cifradoTexto = Base64.encodeToString(
            cifrado,
            Base64.NO_WRAP
        )

        val valor = "$ivTexto:$cifradoTexto"

        val guardado = preferencias.edit()
            .putString(CLAVE_REFRESH, valor)
            .commit()

        check(guardado) {
            "No se pudo guardar la sesión."
        }
    }

    @Synchronized
    fun obtenerRefreshToken(): String? {

        val valor = preferencias.getString(
            CLAVE_REFRESH,
            null
        ) ?: return null

        return try {

            val partes = valor.split(":", limit = 2)

            if (partes.size != 2) {
                limpiar()
                return null
            }

            val iv = Base64.decode(
                partes[0],
                Base64.DEFAULT
            )

            val cifrado = Base64.decode(
                partes[1],
                Base64.DEFAULT
            )

            val cipher = Cipher.getInstance(
                TRANSFORMACION
            )

            cipher.init(
                Cipher.DECRYPT_MODE,
                obtenerClave(),
                GCMParameterSpec(128, iv)
            )

            val original = cipher.doFinal(cifrado)

            String(
                original,
                Charsets.UTF_8
            )

        } catch (e: Exception) {

            // Si la clave ya no existe, fue invalidada
            // o los datos están dañados, descartar
            // la credencial local.
            limpiar()
            null
        }
    }

    @Synchronized
    fun limpiar() {

        preferencias.edit()
            .remove(CLAVE_REFRESH)
            .commit()
    }
}
