package com.example.frutosapp.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.frutosapp.CarritoViewModel
import com.example.frutosapp.VentaViewModel
import java.net.URLEncoder
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.padding

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPago(
    ventaViewModel: VentaViewModel,
    carritoVM: CarritoViewModel,
    onVolver: () -> Unit,
    onPagoExitoso: () -> Unit
) {

    val webpayInicio =
        ventaViewModel.webpayInicio.collectAsStateWithLifecycle().value

    LaunchedEffect(webpayInicio) {
        if (webpayInicio == null) {
            onVolver()
        }
    }

    if (webpayInicio == null) {
        return
    }

// Desde aquí usamos una referencia NO nullable
    val datosWebpay = webpayInicio

    /*
     * Si por alguna razón se llega a esta pantalla
     * sin haber iniciado Webpay, volvemos.
     */
    LaunchedEffect(webpayInicio) {
        if (webpayInicio == null) {
            onVolver()
        }
    }

    if (webpayInicio == null) {
        return
    }

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

        AndroidView(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),

            factory = { context ->

                WebView(context).apply {

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    webViewClient = object : WebViewClient() {

                        override fun onPageFinished(
                            view: WebView?,
                            url: String?
                        ) {
                            super.onPageFinished(view, url)

                            /*
                             * Webpay finalmente vuelve al endpoint
                             * de nuestro backend.
                             *
                             * En ese momento el backend:
                             *
                             * 1. confirma la transacción
                             * 2. crea la venta
                             * 3. descuenta stock
                             * 4. registra el pago
                             */
                            if (
                                url != null &&
                                url.contains("/api/webpay/retorno")
                            ) {

                                ventaViewModel.procesarPagoWebpay(
                                    token = datosWebpay.token
                                ) {

                                    carritoVM.vaciarCarrito()

                                    onPagoExitoso()
                                }
                            }
                        }
                    }

                    /*
                     * Webpay necesita recibir token_ws mediante POST.
                     *
                     * No sirve simplemente abrir:
                     *
                     * loadUrl(webpayInicio.url)
                     *
                     * porque Transbank espera token_ws.
                     */
                    val parametros =
                        "token_ws=" +
                                URLEncoder.encode(
                                    datosWebpay.token,
                                    "UTF-8"
                                )

                    postUrl(
                        datosWebpay.url,
                        parametros.toByteArray(
                            Charsets.UTF_8
                        )
                    )
                }
            }
        )
    }
}