
package com.example.frutosapp.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.frutosapp.VentaViewModel
import com.example.frutosapp.ProductoViewModel
import com.example.frutosapp.api.RetroFistCliente
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaComprobanteVenta(
    ventaViewModel: VentaViewModel,
    productoViewModel: ProductoViewModel,
    onNavigateBack: () -> Unit,
    onCalificarProductos: (ventaId: Long, productoId: Long) -> Unit
) {

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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var descargandoPdf by remember {
        mutableStateOf(false)
    }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CL"))
    }

    // Permite elegir dónde guardar el comprobante PDF.
    val guardarPdf = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/pdf"
        )
    ) { uri ->

        if (uri != null) {

            val idVenta = venta?.idVenta

            if (idVenta != null) {

                scope.launch {

                    descargandoPdf = true

                    try {

                        // Obtener el PDF desde el backend.
                        val response = withContext(Dispatchers.IO) {
                            RetroFistCliente.ventaApi
                                .descargarComprobante(idVenta)
                        }

                        if (!response.isSuccessful) {
                            throw Exception(
                                "Error HTTP ${response.code()}"
                            )
                        }

                        val pdf = response.body()
                            ?: throw Exception(
                                "El servidor no entregó el PDF."
                            )

                        // Guardar el contenido PDF en el archivo
                        // seleccionado por el usuario.
                        withContext(Dispatchers.IO) {

                            context.contentResolver
                                .openOutputStream(uri)
                                ?.use { output ->

                                    pdf.byteStream().use { input ->
                                        input.copyTo(output)
                                    }

                                } ?: throw Exception(
                                "No fue posible guardar el archivo."
                            )
                        }

                        Toast.makeText(
                            context,
                            "Comprobante PDF guardado correctamente",
                            Toast.LENGTH_LONG
                        ).show()

                        // Intentar abrir el PDF guardado.
                        val intent = Intent(Intent.ACTION_VIEW).apply {
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

    // Cargar detalles de la venta seleccionada.
    LaunchedEffect(venta?.idVenta) {
        val idVenta = venta?.idVenta

        if (idVenta != null) {
            ventaViewModel.cargarDetallesVenta(idVenta)
        }
    }

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

            if (venta == null) {

                Text(
                    text = "No hay venta seleccionada.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )

                return@Box
            }

            // Referencia de venta no nula.
            val ventaActual = venta ?: return@Box

            when {

                cargando -> {

                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error != null -> {

                    Text(
                        text = error.orEmpty(),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                else -> {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {

                        // RESUMEN DE LA VENTA
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {

                                Text(
                                    text = "Número de boleta: #${ventaActual.idVenta}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                if (ventaActual.clienteId != null) {

                                    Text(
                                        text = "Cliente ID: ${ventaActual.clienteId}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                } else {

                                    Text(
                                        text = "Cliente: ${ventaActual.nombreInvitado.orEmpty()} ${ventaActual.apellidoInvitado.orEmpty()}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        text = "Correo: ${ventaActual.correoInvitado.orEmpty()}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        text = "Teléfono: ${ventaActual.telefonoInvitado.orEmpty()}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        text = "Dirección: ${ventaActual.direccionInvitado.orEmpty()}",
                                        style = MaterialTheme.typography.bodyMedium
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

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        // NUEVO BOTÓN DE COMPROBANTE PDF
                        Button(
                            onClick = {
                                guardarPdf.launch(
                                    "comprobante_venta_${ventaActual.idVenta}.pdf"
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

                                Text("Ver / descargar comprobante PDF")
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        // DETALLE DE PRODUCTOS
                        Text(
                            text = "Detalle de productos:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            items(detalles) { det ->

                                val subtotalDetalle =
                                    (det.cantidad / 1000.0) *
                                            det.precioUnitario

                                val producto = productos.find {
                                    it.idProductos?.toLong() ==
                                            det.id.productoId
                                }

                                val nombreProducto =
                                    producto?.nomProducto
                                        ?: "Producto no encontrado"

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 1.dp
                                    )
                                ) {

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                    ) {

                                        Text(
                                            text = "Código del producto: ${det.id.productoId}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Spacer(
                                            modifier = Modifier.height(2.dp)
                                        )

                                        Text(
                                            text = "Producto: $nombreProducto",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Spacer(
                                            modifier = Modifier.height(2.dp)
                                        )

                                        Text(
                                            text = "Cantidad: ${formatPeso(det.cantidad)}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Spacer(
                                            modifier = Modifier.height(2.dp)
                                        )

                                        Text(
                                            text = "Precio unitario: ${
                                                currencyFormatter.format(
                                                    det.precioUnitario
                                                )
                                            }",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Spacer(
                                            modifier = Modifier.height(2.dp)
                                        )

                                        Text(
                                            text = "Subtotal: ${
                                                currencyFormatter.format(
                                                    subtotalDetalle
                                                )
                                            }",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        // Mantener calificación de productos.
                                        if (ventaActual.clienteId != null) {

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
