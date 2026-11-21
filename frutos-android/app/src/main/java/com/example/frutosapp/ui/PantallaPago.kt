
package com.example.frutosapp.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.frutosapp.CarritoViewModel
import com.example.frutosapp.VentaViewModel

import java.net.URLEncoder

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPago(
    ventaViewModel: VentaViewModel,
    carritoVM: CarritoViewModel,
    onVolver: () -> Unit,
    onPagoExitoso: () -> Unit
) {

    // ==========================================
    // 1. OBTENER TRANSACCIÓN WEBPAY
    // ==========================================

    val webpayInicio by ventaViewModel
        .webpayInicio
        .collectAsStateWithLifecycle()

    LaunchedEffect(webpayInicio) {
        if (webpayInicio == null) {
            onVolver()
        }
    }

    val datosWebpay = webpayInicio ?: return

    // ==========================================
    // 2. PROTECCIÓN DEL TOKEN
    // ==========================================
    //
    // inicioEnviado:
    // Impide volver a enviar POST con el mismo
    // token durante una recreación de pantalla.
    //
    // retornoProcesado:
    // Evita procesar el retorno dos veces.
    //
    // Se guardan indicadores, nunca el token
    // en registros o almacenamiento adicional.
    // ==========================================

    var inicioEnviado by rememberSaveable(
        datosWebpay.token
    ) {
        mutableStateOf(false)
    }

    var retornoProcesado by rememberSaveable(
        datosWebpay.token
    ) {
        mutableStateOf(false)
    }

    // Solo existe durante esta instancia de
    // composición. Permite identificar cuándo
    // Android recreó la pantalla con un token
    // cuyo POST ya había sido enviado.

    var webViewCreado by remember(
        datosWebpay.token
    ) {
        mutableStateOf(false)
    }

    // ==========================================
    // 3. NAVEGACIÓN
    // ==========================================

    BackHandler {
        onVolver()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Pago Webpay")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->

        // ======================================
        // 4. EVITAR REABRIR TRANSACCIÓN USADA
        // ======================================
        //
        // Si Activity se recreó y el POST
        // ya había sido enviado, no repetirlo.
        //
        // La compra puede seguir pendiente,
        // por lo que no se informa que falló
        // ni se vacía el carrito.
        // ======================================

        if (inicioEnviado && !webViewCreado) {

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    "Esta transacción Webpay " +
                            "ya fue iniciada."
                )

                Text(
                    "La pantalla de pago se reinició. " +
                            "No volveremos a enviar el mismo " +
                            "token para evitar duplicar " +
                            "el intento."
                )

                Text(
                    "Comprueba el estado de tu compra " +
                            "antes de iniciar otro pago."
                )

                Button(
                    onClick = onVolver
                ) {
                    Text("Volver")
                }
            }

        } else {

            // ==================================
            // 5. WEBVIEW DE WEBPAY
            // ==================================

            AndroidView(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),

                factory = { context ->

                    WebView(context).apply {

                        // Marcar esta instancia como
                        // creada antes de cargar Webpay.

                        webViewCreado = true

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true

                        webViewClient =
                            object : WebViewClient() {

                                override fun onPageFinished(
                                    view: WebView?,
                                    url: String?
                                ) {

                                    super.onPageFinished(
                                        view,
                                        url
                                    )

                                    if (url.isNullOrBlank()) {
                                        return
                                    }

                                    // ======================
                                    // 6. IDENTIFICAR RETORNO
                                    // ======================

                                    val esRetornoWebpay =
                                        try {

                                            Uri.parse(url)
                                                .path
                                                ?.trimEnd('/') ==
                                                    "/api/webpay/retorno"

                                        } catch (
                                            e: Exception
                                        ) {

                                            false
                                        }

                                    if (!esRetornoWebpay) {
                                        return
                                    }

                                    // ======================
                                    // 7. EVITAR DUPLICADOS
                                    // ======================

                                    if (retornoProcesado) {

                                        Log.d(
                                            "NATUR_WEBPAY",
                                            "Retorno repetido ignorado."
                                        )

                                        return
                                    }

                                    retornoProcesado = true

                                    Log.d(
                                        "NATUR_WEBPAY",
                                        "Retorno detectado. " +
                                                "Consultando estado."
                                    )

                                    // ======================
                                    // 8. VALIDACIÓN REAL
                                    // ======================
                                    //
                                    // La URL de retorno no
                                    // demuestra pago exitoso.
                                    //
                                    // El ViewModel debe
                                    // comprobar autorización,
                                    // venta y recuperación.
                                    // ======================

                                    ventaViewModel
                                        .procesarPagoWebpay(
                                            token =
                                                datosWebpay.token
                                        ) {

                                            Log.d(
                                                "NATUR_WEBPAY",
                                                "Pago verificado."
                                            )

                                            // Vaciar solamente
                                            // tras confirmación.

                                            carritoVM
                                                .vaciarCarrito()

                                            onPagoExitoso()
                                        }
                                }
                            }

                        // ==========================
                        // 9. ENVIAR TOKEN UNA VEZ
                        // ==========================

                        if (!inicioEnviado) {

                            inicioEnviado = true

                            val parametros =
                                "token_ws=" +
                                        URLEncoder.encode(
                                            datosWebpay.token,
                                            "UTF-8"
                                        )

                            Log.d(
                                "NATUR_WEBPAY",
                                "Iniciando Webpay."
                            )

                            postUrl(
                                datosWebpay.url,
                                parametros.toByteArray(
                                    Charsets.UTF_8
                                )
                            )

                        } else {

                            // No utilizar loadUrl()
                            // ni postUrl() con un
                            // token ya enviado.

                            Log.w(
                                "NATUR_WEBPAY",
                                "Reenvío del token bloqueado."
                            )
                        }
                    }
                }
            )
        }
    }
}
