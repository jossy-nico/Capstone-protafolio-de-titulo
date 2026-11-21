
package com.example.frutosapp.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.frutosapp.VentaViewModel

import org.json.JSONObject

// ==========================================================
// HU-33: CONSULTA DE PEDIDOS PARA COMPRADORES INVITADOS
// ==========================================================
//
// Permite:
// - Consultar pedidos sin iniciar sesión.
// - Ingresar número de orden Webpay.
// - Ingresar código privado de seguimiento.
// - Validar ambos datos mediante venta-api.
// - Mostrar el seguimiento del despacho.
// - Consultar las fechas del pedido.
// - Consultar información del transportista.
// - Actualizar el seguimiento.
//
// No modifica estados ni datos de la venta.
//
// ==========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConsultarPedido(
    ventaViewModel: VentaViewModel,
    onVolver: () -> Unit
) {

    // ======================================================
    // 1. COLORES
    // ======================================================

    val verde = Color(0xFF008B36)
    val verdeClaro = Color(0xFFE8F5EC)
    val gris = Color(0xFF95A19B)
    val rojo = Color(0xFFB3261E)

    val contexto = LocalContext.current

    // ======================================================
    // 2. DATOS INGRESADOS POR EL COMPRADOR
    // ======================================================

    var numeroOrden by remember {
        mutableStateOf("")
    }

    var codigoPrivado by remember {
        mutableStateOf("")
    }

    var mostrarCodigo by remember {
        mutableStateOf(false)
    }

    // ======================================================
    // 3. ESTADOS DEL VIEWMODEL
    // ======================================================

    val seguimiento by ventaViewModel
        .seguimientoInvitadoJson
        .collectAsStateWithLifecycle()

    val cargando by ventaViewModel
        .cargandoSeguimientoInvitado
        .collectAsStateWithLifecycle()

    val error by ventaViewModel
        .errorSeguimientoInvitado
        .collectAsStateWithLifecycle()

    // ======================================================
    // 4. LIMPIAR CONSULTA AL SALIR
    // ======================================================

    DisposableEffect(Unit) {

        onDispose {
            ventaViewModel.limpiarSeguimientoInvitado()
        }
    }

    // ======================================================
    // 5. CONVERTIR RESPUESTA JSON
    // ======================================================

    val datos = remember(seguimiento) {

        seguimiento?.let { contenido ->

            runCatching {
                JSONObject(contenido)
            }.getOrNull()
        }
    }

    // ======================================================
    // 6. VALIDACIONES DEL FORMULARIO
    // ======================================================

    val ordenValida =
        numeroOrden.trim().isNotBlank() &&
                numeroOrden.trim().length <= 50

    val codigoValido =
        codigoPrivado.trim().matches(
            Regex("NC-[A-Za-z0-9_-]{32}")
        )

    // ======================================================
    // 7. CONSULTAR BACKEND
    // ======================================================

    fun consultar() {

        if (
            ordenValida &&
            codigoValido &&
            !cargando
        ) {

            ventaViewModel.cargarSeguimientoInvitado(
                numeroOrden = numeroOrden.trim(),
                codigoSeguimiento = codigoPrivado.trim()
            )
        }
    }

    // ======================================================
    // 8. PANTALLA PRINCIPAL
    // ======================================================

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Consultar mi pedido")
                },

                navigationIcon = {

                    TextButton(
                        onClick = onVolver
                    ) {
                        Text(
                            text = "Volver",
                            color = verde
                        )
                    }
                }
            )
        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)

        ) {

            // ==============================================
            // ENCABEZADO
            // ==============================================

            Text(
                text = "NATUR COSECHA",
                color = verde,
                style =
                    MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Seguimiento de compras",
                style =
                    MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "¿Compraste como invitado? " +
                        "Ingresa los datos de tu compra " +
                        "para conocer su preparación, " +
                        "despacho y entrega.",
                style =
                    MaterialTheme.typography.bodyMedium
            )

            // ==============================================
            // FORMULARIO
            // ==============================================

            Card(

                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(16.dp),

                colors = CardDefaults.cardColors(
                    containerColor =
                        Color(0xFFF7F8F7)
                )

            ) {

                Column(

                    modifier = Modifier.padding(16.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(14.dp)

                ) {

                    Text(
                        text = "Datos del pedido",
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // ======================================
                    // NÚMERO DE ORDEN
                    // ======================================

                    OutlinedTextField(

                        value = numeroOrden,

                        onValueChange = { nuevoValor ->

                            numeroOrden =
                                nuevoValor.take(50)
                        },

                        modifier = Modifier.fillMaxWidth(),

                        label = {
                            Text("Número de orden")
                        },

                        placeholder = {
                            Text("ORD-...")
                        },

                        singleLine = true,

                        keyboardOptions = KeyboardOptions(
                            capitalization =
                                KeyboardCapitalization.Characters
                        )
                    )

                    // ======================================
                    // CÓDIGO PRIVADO
                    // ======================================

                    OutlinedTextField(

                        value = codigoPrivado,

                        onValueChange = { nuevoValor ->

                            codigoPrivado =
                                nuevoValor.take(35)
                        },

                        modifier = Modifier.fillMaxWidth(),

                        label = {
                            Text(
                                "Código privado de seguimiento"
                            )
                        },

                        placeholder = {
                            Text("NC-...")
                        },

                        singleLine = true,

                        visualTransformation =
                            if (mostrarCodigo) {

                                VisualTransformation.None

                            } else {

                                PasswordVisualTransformation()
                            },

                        trailingIcon = {

                            TextButton(

                                onClick = {
                                    mostrarCodigo = !mostrarCodigo
                                }

                            ) {

                                Text(
                                    text = if (mostrarCodigo) {
                                        "Ocultar"
                                    } else {
                                        "Ver"
                                    }
                                )
                            }
                        }
                    )

                    // ======================================
                    // INFORMACIÓN
                    // ======================================

                    Text(
                        text = "Encontrarás estos datos " +
                                "en la confirmación de tu compra.",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )

                    // ======================================
                    // BOTÓN CONSULTAR
                    // ======================================

                    Button(

                        onClick = {
                            consultar()
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),

                        enabled =
                            ordenValida &&
                                    codigoValido &&
                                    !cargando,

                        colors = ButtonDefaults.buttonColors(
                            containerColor = verde
                        )

                    ) {

                        Text(
                            "🚚 Consultar seguimiento"
                        )
                    }

                } // Cierre Column del formulario

            } // Cierre Card del formulario

            // ==============================================
            // CARGANDO
            // ==============================================

            if (cargando) {

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = verde
                )

                Text(
                    text = "Consultando tu pedido..."
                )
            }

            // ==============================================
            // ERROR
            // ==============================================

            if (!error.isNullOrBlank()) {

                Card(

                    colors = CardDefaults.cardColors(
                        containerColor =
                            Color(0xFFFFF1F1)
                    )

                ) {

                    Text(
                        text = error.orEmpty(),
                        modifier = Modifier.padding(16.dp),
                        color = rojo
                    )
                }
            }

            // ==============================================
            // RESULTADO DE SEGUIMIENTO
            // ==============================================

            if (
                datos != null &&
                error == null
            ) {

                val idVenta =
                    datos.optLong("ventaId", -1L)

                val estado =
                    datos.optString("estadoActual", "")

                val mensaje =
                    datos.optString("mensajeEstado", "")

                // ==========================================
                // OBTENER VALORES OPCIONALES DEL JSON
                // ==========================================

                fun campo(clave: String): String? {

                    return datos
                        .optString(clave, "")
                        .trim()
                        .takeUnless { valor ->

                            valor.isBlank() ||
                                    valor.equals(
                                        "null",
                                        ignoreCase = true
                                    )
                        }
                }

                // ==========================================
                // ETAPA ACTUAL
                // ==========================================

                val avance = when (estado) {

                    "PAGADO" -> 0

                    "EN_PREPARACION" -> 1

                    "ENVIADO" -> 2

                    "ENTREGADO" -> 3

                    else -> -1
                }

                val cancelado =
                    estado == "CANCELADO"

                val titulos = listOf(
                    "Pago confirmado",
                    "Preparando tu pedido",
                    "Pedido en camino",
                    "Entregado en tu domicilio"
                )

                val dibujos = listOf(
                    "💳",
                    "📦",
                    "🚚",
                    "🏠"
                )

                val fechas = listOf(
                    campo("fechaPago"),
                    campo("fechaPreparacion"),
                    campo("fechaEnvio"),
                    campo("fechaEntrega")
                )

                HorizontalDivider()

                // ==========================================
                // ENCABEZADO DEL PEDIDO
                // ==========================================

                Text(
                    text = "Recorrido de tu pedido",
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Pedido #$idVenta",
                    style =
                        MaterialTheme.typography.titleMedium
                )

                if (mensaje.isNotBlank()) {

                    Text(
                        text = mensaje,
                        color = if (cancelado) {
                            rojo
                        } else {
                            verde
                        }
                    )
                }

                // ==========================================
                // PEDIDO CANCELADO
                // ==========================================

                if (cancelado) {

                    Card(

                        colors = CardDefaults.cardColors(
                            containerColor =
                                Color(0xFFFFEBEE)
                        )

                    ) {

                        Text(
                            text = "Este pedido fue cancelado.",
                            modifier =
                                Modifier.padding(16.dp),
                            color = rojo
                        )
                    }
                }

                // ==========================================
                // LÍNEA DE TIEMPO
                // ==========================================

                Column(

                    verticalArrangement =
                        Arrangement.spacedBy(0.dp)

                ) {

                    titulos.forEachIndexed { indice, titulo ->

                        val completado =
                            indice <= avance &&
                                    !cancelado

                        val esActual =
                            indice == avance &&
                                    !cancelado

                        Row(

                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 94.dp),

                            verticalAlignment =
                                Alignment.Top

                        ) {

                            // ==================================
                            // PUNTOS CONECTADOS
                            // ==================================

                            Column(

                                horizontalAlignment =
                                    Alignment.CenterHorizontally

                            ) {

                                Box(

                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(
                                            color =
                                                if (completado) {
                                                    verde
                                                } else {
                                                    gris
                                                },
                                            shape = CircleShape
                                        ),

                                    contentAlignment =
                                        Alignment.Center

                                ) {

                                    Text(
                                        text =
                                            if (completado) {
                                                "✓"
                                            } else {
                                                "•"
                                            },
                                        color = Color.White
                                    )
                                }

                                if (indice < 3) {

                                    Box(

                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(65.dp)
                                            .background(
                                                if (
                                                    indice < avance &&
                                                    !cancelado
                                                ) {
                                                    verde
                                                } else {
                                                    gris
                                                }
                                            )
                                    )
                                }
                            }

                            Spacer(
                                modifier =
                                    Modifier.width(14.dp)
                            )

                            // ==================================
                            // INFORMACIÓN DE ETAPA
                            // ==================================

                            Column(

                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 2.dp)

                            ) {

                                Text(
                                    text = titulo,
                                    fontWeight =
                                        if (esActual) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Medium
                                        },
                                    color =
                                        if (completado) {
                                            Color(0xFF183A28)
                                        } else {
                                            gris
                                        }
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(5.dp)
                                )

                                Text(
                                    text = when {

                                        esActual ->
                                            "Estamos aquí"

                                        completado ->
                                            "Completado"

                                        else ->
                                            "Pendiente"
                                    },

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall,

                                    color =
                                        if (esActual) {
                                            verde
                                        } else {
                                            Color.DarkGray
                                        }
                                )

                                fechas[indice]?.let { fecha ->

                                    Spacer(
                                        modifier =
                                            Modifier.height(4.dp)
                                    )

                                    Text(
                                        text = fecha,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .labelSmall
                                    )
                                }
                            }

                            // ==================================
                            // DIBUJO DE LA ETAPA
                            // ==================================

                            Surface(

                                shape =
                                    RoundedCornerShape(14.dp),

                                color =
                                    if (completado) {
                                        verdeClaro
                                    } else {
                                        Color(0xFFF4F5F4)
                                    }

                            ) {

                                Text(
                                    text = dibujos[indice],
                                    modifier =
                                        Modifier.padding(14.dp),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .headlineSmall
                                )
                            }

                        } // Cierre Row de la etapa

                    } // Cierre forEachIndexed

                } // Cierre Column de línea de tiempo

                HorizontalDivider()

                // ==========================================
                // INFORMACIÓN DEL TRANSPORTISTA
                // ==========================================

                Text(
                    text = "Información del transporte",
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Transportista: " +
                            (campo("transportista")
                                ?: "Sin asignar")
                )

                Text(
                    text = "Número de seguimiento: " +
                            (campo("numeroSeguimientoTransporte")
                                ?: "No disponible")
                )

                // ==========================================
                // ENLACE DE TRANSPORTE
                // ==========================================

                val url =
                    campo("urlSeguimientoTransporte")

                val urlValida = remember(url) {

                    runCatching {

                        val uri =
                            Uri.parse(url.orEmpty())

                        uri.scheme.equals(
                            "https",
                            ignoreCase = true
                        ) &&
                                !uri.host.isNullOrBlank()

                    }.getOrDefault(false)
                }

                Button(

                    onClick = {

                        if (
                            urlValida &&
                            url != null
                        ) {

                            try {

                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(url)
                                )

                                contexto.startActivity(intent)

                            } catch (
                                e: ActivityNotFoundException
                            ) {

                                // No existe una aplicación
                                // disponible para abrir el enlace.

                            } catch (
                                e: SecurityException
                            ) {

                                // El dispositivo no permitió
                                // abrir el enlace.

                            }
                        }
                    },

                    modifier = Modifier.fillMaxWidth(),

                    enabled = urlValida,

                    colors = ButtonDefaults.buttonColors(
                        containerColor = verde
                    )

                ) {

                    Text("🚚 Seguir mi entrega")
                }

                // ==========================================
                // ACTUALIZAR SEGUIMIENTO
                // ==========================================

                OutlinedButton(

                    onClick = {
                        consultar()
                    },

                    modifier = Modifier.fillMaxWidth(),

                    enabled = !cargando

                ) {

                    Text("🔄 Actualizar seguimiento")
                }

            } // Cierre resultado del seguimiento

            // ==============================================
            // MENSAJE DE SEGURIDAD
            // ==============================================

            HorizontalDivider()

            Text(
                text = "🔒 Tu código es privado. " +
                        "No lo compartas con otras personas.",
                style =
                    MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

        } // Cierre Column principal

    } // Cierre Scaffold

} // Cierre PantallaConsultarPedido
