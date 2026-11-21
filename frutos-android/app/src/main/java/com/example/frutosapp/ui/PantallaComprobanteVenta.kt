
package com.example.frutosapp.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.frutosapp.VentaViewModel
import com.example.frutosapp.ProductoViewModel
import com.example.frutosapp.api.RetroFistCliente

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.json.JSONObject

import java.text.NumberFormat
import java.util.Locale


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context


// ============================================================
// PANTALLA 1: COMPROBANTE DE VENTA
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun PantallaComprobanteVenta(
    ventaViewModel: VentaViewModel,
    productoViewModel: ProductoViewModel,
    onNavigateBack: () -> Unit,
    onCalificarProductos: (
        ventaId: Long,
        productoId: Long
    ) -> Unit,
    onVerSeguimiento: (Long) -> Unit
) {

    // ==========================================
    // ESTADOS DE LA COMPRA
    // ==========================================

    val venta by ventaViewModel.ventaSeleccionada
        .collectAsStateWithLifecycle()

    val detalles by ventaViewModel.detallesVenta
        .collectAsStateWithLifecycle()

    val productos by productoViewModel.productos
        .collectAsStateWithLifecycle()

    val cargando by ventaViewModel.cargando
        .collectAsStateWithLifecycle()

    val error by ventaViewModel.errorMensaje
        .collectAsStateWithLifecycle()

    // ==========================================
    // ESTADOS WEBPAY
    // ==========================================

    val compraWebpayJson by ventaViewModel
        .compraWebpayRecuperada
        .collectAsStateWithLifecycle()

    val ventaIdWebpay by ventaViewModel
        .ventaIdWebpayConfirmada
        .collectAsStateWithLifecycle()

    val tokenWebpay by ventaViewModel
        .tokenComprobanteWebpay
        .collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var descargandoPdf by remember {
        mutableStateOf(false)
    }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(
            Locale("es", "CL")
        )
    }

    // ==========================================
    // INTERPRETAR COMPRA WEBPAY
    // ==========================================

    val compraWebpay = remember(compraWebpayJson) {
        compraWebpayJson?.let { contenido ->
            runCatching {
                JSONObject(contenido)
            }.getOrNull()
        }
    }

    val esCompraWebpay =
        compraWebpay != null && ventaIdWebpay != null

    val idVentaActual = if (esCompraWebpay) {
        ventaIdWebpay
    } else {
        venta?.idVenta
    }

    val detallesWebpay = remember(compraWebpayJson) {

        val resultado = mutableListOf<JSONObject>()
        val array = compraWebpay?.optJSONArray("detalles")

        if (array != null) {
            for (indice in 0 until array.length()) {
                val detalle = array.optJSONObject(indice)
                if (detalle != null) {
                    resultado.add(detalle)
                }
            }
        }

        resultado.toList()
    }

    // ==========================================
    // GUARDAR Y ABRIR PDF
    // ==========================================

    val guardarPdf = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/pdf"
        )
    ) { uri ->

        if (uri != null) {

            val idVenta = idVentaActual

            if (idVenta == null) {

                Toast.makeText(
                    context,
                    "No se encontró la venta.",
                    Toast.LENGTH_LONG
                ).show()

            } else {

                scope.launch {

                    descargandoPdf = true

                    try {

                        val response = withContext(
                            Dispatchers.IO
                        ) {

                            if (esCompraWebpay) {

                                // Compra Webpay inmediata.
                                // Se conserva el token de la transacción.

                                val token = tokenWebpay

                                if (token.isNullOrBlank()) {
                                    throw IllegalStateException(
                                        "No está disponible el token del comprobante."
                                    )
                                }

                                RetroFistCliente.ventaApi
                                    .descargarComprobanteInvitado(
                                        mapOf("token" to token)
                                    )

                            } else {

                                // Compra del historial.
                                // Utiliza JWT CLIENTE, no ADMIN.

                                if (!RetroFistCliente.existeSesionJwt()) {
                                    throw IllegalStateException(
                                        "Debes iniciar sesión para descargar tu comprobante."
                                    )
                                }

                                RetroFistCliente.ventaApi
                                    .descargarComprobanteMiCompra(
                                        idVenta
                                    )
                            }
                        }

                        if (!response.isSuccessful) {

                            val mensaje = when (response.code()) {

                                401, 403 ->
                                    "No tienes autorización para descargar este comprobante."

                                410 ->
                                    "Finalizó el período de descarga inmediata. El pago permanece registrado."

                                404 ->
                                    "No se encontró el comprobante."

                                409 ->
                                    "El pago todavía no está confirmado."

                                else ->
                                    "Error HTTP ${response.code()}"
                            }

                            throw IllegalStateException(mensaje)
                        }

                        val pdf = response.body()
                            ?: throw IllegalStateException(
                                "El servidor no entregó el PDF."
                            )

                        withContext(Dispatchers.IO) {

                            context.contentResolver
                                .openOutputStream(uri)
                                ?.use { output ->

                                    pdf.byteStream().use { input ->
                                        input.copyTo(output)
                                    }

                                } ?: throw IllegalStateException(
                                "No fue posible guardar el archivo."
                            )
                        }

                        Toast.makeText(
                            context,
                            "Comprobante PDF guardado correctamente",
                            Toast.LENGTH_LONG
                        ).show()

                        val intent = Intent(
                            Intent.ACTION_VIEW
                        ).apply {

                            setDataAndType(
                                uri,
                                "application/pdf"
                            )

                            addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }

                        try {

                            context.startActivity(intent)

                        } catch (e: ActivityNotFoundException) {

                            Toast.makeText(
                                context,
                                "PDF guardado. No hay un visor PDF instalado.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (e: Exception) {

                        Toast.makeText(
                            context,
                            "Error al descargar PDF: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()

                    } finally {

                        descargandoPdf = false
                    }
                }
            }
        }
    }

    // ==========================================
    // CARGAR DETALLES DEL HISTORIAL
    // ==========================================

    LaunchedEffect(
        venta?.idVenta,
        ventaIdWebpay
    ) {

        val id = venta?.idVenta

        if (id != null && ventaIdWebpay == null) {
            ventaViewModel.cargarDetallesVenta(id)
        }
    }

    // ==========================================
    // INTERFAZ DEL COMPROBANTE
    // ==========================================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Comprobante de compra")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            if (idVentaActual == null) {

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    if (cargando) {

                        CircularProgressIndicator()

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text("Recuperando compra...")

                    } else {

                        Text(
                            error ?: "No hay venta seleccionada."
                        )
                    }
                }

            } else {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {

                    // ==================================
                    // RESUMEN
                    // ==================================

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {

                            Text(
                                text = "Comprobante de venta #$idVentaActual",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            if (esCompraWebpay) {

                                val orden = compraWebpay
                                    ?.optString("buyOrder", "")
                                    .orEmpty()

                                val estadoPago = compraWebpay
                                    ?.optString("estadoPago", "")
                                    .orEmpty()

                                val estadoPedido = compraWebpay
                                    ?.optString("estadoPedido", "")
                                    .orEmpty()

                                val total = compraWebpay
                                    ?.optDouble("total", 0.0)
                                    ?: 0.0

                                Text("Orden: $orden")
                                Text("Estado del pago: $estadoPago")
                                Text("Estado del pedido: $estadoPedido")

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )

                                Text(
                                    text = "Total: ${
                                        currencyFormatter.format(total)
                                    }",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                            } else {

                                val ventaActual = venta

                                if (ventaActual != null) {

                                    if (ventaActual.clienteId != null) {

                                        Text(
                                            "Cliente ID: ${ventaActual.clienteId}"
                                        )

                                    } else {

                                        Text(
                                            "Cliente: ${
                                                ventaActual.nombreInvitado.orEmpty()
                                            } ${
                                                ventaActual.apellidoInvitado.orEmpty()
                                            }"
                                        )

                                        Text(
                                            "Correo: ${
                                                ventaActual.correoInvitado.orEmpty()
                                            }"
                                        )

                                        Text(
                                            "Teléfono: ${
                                                ventaActual.telefonoInvitado.orEmpty()
                                            }"
                                        )

                                        Text(
                                            "Dirección: ${
                                                ventaActual.direccionInvitado.orEmpty()
                                            }"
                                        )
                                    }

                                    Spacer(
                                        modifier = Modifier.height(8.dp)
                                    )

                                    Text(
                                        text = "Total: ${
                                            currencyFormatter.format(
                                                ventaActual.totalVenta
                                            )
                                        }",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    // ==========================================
                    // HU-33: CÓDIGO DE SEGUIMIENTO PARA INVITADOS
                    // ==========================================

                    val numeroOrdenInvitado = compraWebpay
                        ?.optString("buyOrder", "")
                        .orEmpty()

                    val codigoSeguimientoInvitado = compraWebpay
                        ?.optString("codigoSeguimiento", "")
                        .orEmpty()

                    if (
                        esCompraWebpay &&
                        numeroOrdenInvitado.isNotBlank() &&
                        codigoSeguimientoInvitado.matches(
                            Regex("NC-[A-Za-z0-9_-]{32}")
                        )
                    ) {

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {

                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(12.dp)
                            ) {

                                Text(
                                    text = "🚚 Seguimiento de tu compra",
                                    style =
                                        MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    "Guarda estos datos para consultar " +
                                            "tu pedido cuando quieras, " +
                                            "sin necesidad de iniciar sesión."
                                )

                                HorizontalDivider()

                                Text(
                                    "Número de orden",
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    numeroOrdenInvitado,
                                    style =
                                        MaterialTheme.typography.bodyLarge
                                )

                                Text(
                                    "Código privado de seguimiento",
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    codigoSeguimientoInvitado,
                                    style =
                                        MaterialTheme.typography.bodyMedium
                                )

                                Button(
                                    onClick = {

                                        val datos =
                                            "Natur Cosecha\n" +
                                                    "Número de orden: " +
                                                    numeroOrdenInvitado + "\n" +
                                                    "Código de seguimiento: " +
                                                    codigoSeguimientoInvitado

                                        val clipboard =
                                            context.getSystemService(
                                                Context.CLIPBOARD_SERVICE
                                            ) as ClipboardManager

                                        val clip = ClipData.newPlainText(
                                            "Seguimiento Natur Cosecha",
                                            datos
                                        )

                                        clipboard.setPrimaryClip(clip)

                                        Toast.makeText(
                                            context,
                                            "Datos copiados. Guárdalos " +
                                                    "en un lugar privado.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("📋 Copiar datos del seguimiento")
                                }

                                Text(
                                    text =
                                        "Este código es privado. " +
                                                "No lo compartas con otras personas.",
                                    style =
                                        MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )
                    }


                    // ==================================
                    // HU-32: VER SEGUIMIENTO DEL DESPACHO
                    // ==================================
                    //
                    // Disponible para clientes con JWT.
                    // El backend comprueba la titularidad.
                    // ==================================

                    if (
                        RetroFistCliente.existeSesionJwt() &&
                        idVentaActual != null
                    ) {

                        OutlinedButton(
                            onClick = {
                                onVerSeguimiento(idVentaActual)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {

                            Text("🚚  Ver seguimiento del despacho")
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )
                    }


                    // ==================================
                    // BOTÓN PDF
                    // ==================================

                    Button(
                        onClick = {
                            guardarPdf.launch(
                                "comprobante_venta_$idVentaActual.pdf"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !descargandoPdf
                    ) {

                        if (descargandoPdf) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text("Descargando PDF...")

                        } else {

                            Text(
                                "Ver / descargar comprobante PDF"
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    // ==================================
                    // DETALLE DE PRODUCTOS
                    // ==================================

                    Text(
                        text = "Detalle de productos:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    if (cargando) {

                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )
                    }

                    if (error != null && !esCompraWebpay) {

                        Text(
                            text = error.orEmpty(),
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        // ==========================
                        // PRODUCTOS WEBPAY
                        // ==========================

                        if (esCompraWebpay) {

                            items(detallesWebpay) { det ->

                                val identificador =
                                    det.optJSONObject("id")

                                val productoId =
                                    identificador?.optLong(
                                        "productoId",
                                        0L
                                    ) ?: 0L

                                val cantidad =
                                    det.optDouble(
                                        "cantidad",
                                        0.0
                                    )

                                val precioUnitario =
                                    det.optDouble(
                                        "precioUnitario",
                                        0.0
                                    )

                                val subtotal =
                                    det.optDouble(
                                        "subtotal",
                                        0.0
                                    )

                                val producto =
                                    productos.find {
                                        it.idProductos?.toLong() ==
                                                productoId
                                    }

                                val nombreProducto =
                                    producto?.nomProducto
                                        ?: "Producto #$productoId"

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation =
                                        CardDefaults.cardElevation(
                                            defaultElevation = 1.dp
                                        )
                                ) {

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {

                                        Text(
                                            "Producto: $nombreProducto",
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Text(
                                            "Código: $productoId"
                                        )

                                        Text(
                                            "Cantidad: ${cantidad.toInt()} g"
                                        )

                                        Text(
                                            "Precio por kilo: ${
                                                currencyFormatter.format(
                                                    precioUnitario
                                                )
                                            }"
                                        )

                                        Text(
                                            "Subtotal: ${
                                                currencyFormatter.format(
                                                    subtotal
                                                )
                                            }"
                                        )
                                    }
                                }
                            }

                        } else {

                            // ==========================
                            // PRODUCTOS DEL HISTORIAL
                            // ==========================

                            items(detalles) { det ->

                                val producto =
                                    productos.find {
                                        it.idProductos?.toLong() ==
                                                det.id.productoId
                                    }

                                val nombreProducto =
                                    producto?.nomProducto
                                        ?: "Producto no encontrado"

                                val subtotal =
                                    (det.cantidad / 1000.0) *
                                            det.precioUnitario

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation =
                                        CardDefaults.cardElevation(
                                            defaultElevation = 1.dp
                                        )
                                ) {

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {

                                        Text(
                                            "Código del producto: ${
                                                det.id.productoId
                                            }"
                                        )

                                        Text(
                                            "Producto: $nombreProducto",
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Text(
                                            "Cantidad: ${
                                                det.cantidad
                                            } g"
                                        )

                                        Text(
                                            "Precio unitario: ${
                                                currencyFormatter.format(
                                                    det.precioUnitario
                                                )
                                            }"
                                        )

                                        Text(
                                            "Subtotal: ${
                                                currencyFormatter.format(
                                                    subtotal
                                                )
                                            }"
                                        )

                                        // Calificación del comprador registrado.

                                        val ventaActual = venta

                                        if (ventaActual?.clienteId != null) {

                                            Spacer(
                                                modifier = Modifier.height(8.dp)
                                            )

                                            Button(
                                                onClick = {
                                                    onCalificarProductos(
                                                        ventaActual.idVenta,
                                                        det.id.productoId
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Calificar producto")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// PANTALLA 2: SEGUIMIENTO DEL DESPACHO - HU-32
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSeguimientoPedido(
    idVenta: Long,
    ventaViewModel: VentaViewModel,
    onVolver: () -> Unit
) {

    val verde = Color(0xFF218554)
    val verdeClaro = Color(0xFFE7F4EB)
    val gris = Color(0xFF9BA5A0)

    val contexto = LocalContext.current

    // ==========================================
    // DATOS REALES DEL BACKEND
    // ==========================================

    val seguimiento by ventaViewModel.seguimientoPedidoJson
        .collectAsStateWithLifecycle()

    val cargando by ventaViewModel.cargandoSeguimiento
        .collectAsStateWithLifecycle()

    val error by ventaViewModel.errorSeguimiento
        .collectAsStateWithLifecycle()

    LaunchedEffect(idVenta) {
        ventaViewModel.cargarSeguimientoPedido(idVenta)
    }

    val datos = remember(seguimiento) {
        seguimiento?.let { contenido ->
            runCatching {
                JSONObject(contenido)
            }.getOrNull()
        }
    }

    val estado = datos
        ?.optString("estadoActual")
        .orEmpty()

    val mensaje = datos
        ?.optString("mensajeEstado")
        .orEmpty()

    // ==========================================
    // CAMPOS OPCIONALES
    // ==========================================

    fun datoOpcional(clave: String): String? {

        val valor = datos
            ?.optString(clave)
            .orEmpty()
            .trim()

        return valor.takeUnless {
            it.isEmpty() ||
                    it.equals("null", ignoreCase = true)
        }
    }

    val transportista =
        datoOpcional("transportista")

    val numeroSeguimiento =
        datoOpcional("numeroSeguimientoTransporte")

    val urlSeguimiento =
        datoOpcional("urlSeguimientoTransporte")

    val urlValida = remember(urlSeguimiento) {

        runCatching {

            val uri = Uri.parse(
                urlSeguimiento.orEmpty()
            )

            uri.scheme.equals(
                "https",
                ignoreCase = true
            ) && !uri.host.isNullOrBlank()

        }.getOrDefault(false)
    }

    // ==========================================
    // ETAPAS DEL DESPACHO
    // ==========================================

    val titulos = listOf(
        "Pago confirmado",
        "Preparando tu pedido",
        "Pedido en camino",
        "Entregado en tu domicilio"
    )

    val descripciones = listOf(
        "Tu pago fue aprobado correctamente",
        "Estamos preparando tus productos",
        "Tu pedido fue despachado",
        "Tu pedido llegó a destino"
    )

    // Dibujos sin depender de material-icons-extended.

    val dibujos = listOf(
        "💳",
        "📦",
        "🚚",
        "🏠"
    )

    val fechas = listOf(
        datoOpcional("fechaPago"),
        datoOpcional("fechaPreparacion"),
        datoOpcional("fechaEnvio"),
        datoOpcional("fechaEntrega")
    )

    val avance = when (estado) {
        "PAGADO" -> 0
        "EN_PREPARACION" -> 1
        "ENVIADO" -> 2
        "ENTREGADO" -> 3
        else -> -1
    }

    // ==========================================
    // INTERFAZ VISUAL
    // ==========================================

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text("Seguimiento del despacho")
                },
                navigationIcon = {
                    TextButton(
                        onClick = onVolver
                    ) {
                        Text("Volver")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "NATUR COSECHA",
                color = verde,
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = "Pedido #$idVenta",
                style = MaterialTheme.typography.headlineMedium
            )

            // ==================================
            // CARGA
            // ==================================

            if (cargando) {

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ==================================
            // ERROR
            // ==================================

            if (error != null) {

                Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )

                OutlinedButton(
                    onClick = {
                        ventaViewModel
                            .cargarSeguimientoPedido(idVenta)
                    }
                ) {
                    Text("Reintentar")
                }
            }

            // ==================================
            // DATOS DEL PEDIDO
            // ==================================

            if (datos != null && error == null) {

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (estado == "CANCELADO") {
                        Color(0xFFFFEBEE)
                    } else {
                        verdeClaro
                    }
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {

                        Text(
                            text = estado.replace("_", " "),
                            color = if (estado == "CANCELADO") {
                                Color(0xFFB3261E)
                            } else {
                                verde
                            },
                            style =
                                MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = mensaje,
                            color = if (estado == "CANCELADO") {
                                Color(0xFFB3261E)
                            } else {
                                Color(0xFF294535)
                            }
                        )
                    }
                }

                Text(
                    text = "Recorrido de tu pedido",
                    style = MaterialTheme.typography.titleLarge
                )

                // ==================================
                // CANCELADO / PENDIENTE DE PAGO
                // ==================================

                if (estado == "CANCELADO") {

                    Text(
                        text = "El despacho fue cancelado. " +
                                "No se mostrarán etapas futuras como completadas.",
                        color = MaterialTheme.colorScheme.error
                    )

                } else if (estado == "PENDIENTE_PAGO") {

                    Text(
                        "El despacho comenzará cuando se confirme el pago."
                    )

                } else {

                    // ==================================
                    // RECORRIDO CON PUNTOS Y DIBUJOS
                    // ==================================

                    titulos.forEachIndexed { indice, titulo ->

                        val completado = indice <= avance
                        val actual = indice == avance

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement =
                                Arrangement.spacedBy(14.dp)
                        ) {

                            // PUNTO Y CONEXIÓN

                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(
                                            if (completado) verde else gris,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {

                                    if (completado) {

                                        Icon(
                                            imageVector =
                                                Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )

                                    } else {

                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    Color.White,
                                                    CircleShape
                                                )
                                        )
                                    }
                                }

                                if (indice < titulos.lastIndex) {

                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(68.dp)
                                            .background(
                                                if (indice < avance) {
                                                    verde
                                                } else {
                                                    gris
                                                }
                                            )
                                    )
                                }
                            }

                            // DESCRIPCIÓN DE LA ETAPA

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement =
                                    Arrangement.spacedBy(4.dp)
                            ) {

                                Text(
                                    text = titulo,
                                    style =
                                        MaterialTheme.typography.titleMedium,
                                    color = if (completado) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        gris
                                    }
                                )

                                Text(
                                    text = if (completado) {
                                        descripciones[indice]
                                    } else {
                                        "Pendiente"
                                    },
                                    style =
                                        MaterialTheme.typography.bodySmall
                                )

                                if (actual) {

                                    Text(
                                        text = "● Estamos aquí",
                                        color = verde,
                                        style =
                                            MaterialTheme.typography.labelMedium
                                    )
                                }

                                if (
                                    completado &&
                                    fechas[indice] != null
                                ) {

                                    Text(
                                        text = fechas[indice]
                                            .orEmpty()
                                            .replace("T", " · "),
                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant,
                                        style =
                                            MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            // DIBUJO DE LA ETAPA

                            Surface(
                                color = if (completado) {
                                    verdeClaro
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {

                                Text(
                                    text = dibujos[indice],
                                    style =
                                        MaterialTheme.typography
                                            .headlineMedium,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // ==================================
                // INFORMACIÓN DEL TRANSPORTE
                // ==================================

                Text(
                    text = "Información del transporte",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    "Transportista: ${
                        transportista ?: "Sin asignar"
                    }"
                )

                Text(
                    "Número de seguimiento: ${
                        numeroSeguimiento ?: "No disponible"
                    }"
                )

                // ==================================
                // SEGUIMIENTO EXTERNO
                // ==================================

                Button(
                    onClick = {

                        if (urlValida) {

                            try {

                                contexto.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(
                                            urlSeguimiento.orEmpty()
                                        )
                                    )
                                )

                            } catch (e: Exception) {

                                Toast.makeText(
                                    contexto,
                                    "No se pudo abrir el seguimiento.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = urlValida,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🚚  Seguir mi entrega")
                }

                // ==================================
                // ACTUALIZAR ESTADO
                // ==================================

                OutlinedButton(
                    onClick = {
                        ventaViewModel
                            .cargarSeguimientoPedido(idVenta)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔄 Actualizar seguimiento")
                }
            }
        }
    }
}
