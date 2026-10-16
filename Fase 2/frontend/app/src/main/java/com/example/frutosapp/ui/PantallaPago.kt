package com.example.frutosapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.frutosapp.CarritoViewModel
import com.example.frutosapp.VentaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPago(
    ventaViewModel: VentaViewModel,
    carritoVM: CarritoViewModel,
    onVolver: () -> Unit,
    onPagoExitoso: () -> Unit
) {
    // Tomamos tanto la venta creada como la seleccionada
    val ventaCreada by ventaViewModel.ventaCreada.collectAsStateWithLifecycle()
    val ventaSeleccionada by ventaViewModel.ventaSeleccionada.collectAsStateWithLifecycle()

    // Usamos primero la seleccionada (por historial), si no, la recién creada
    val ventaMostrada = ventaSeleccionada ?: ventaCreada

    val cargando by ventaViewModel.cargando.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    //  Total que se muestra en la pantalla
    val total = ventaMostrada?.totalVenta ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pago") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total a pagar",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "$${"%.0f".format(total)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (ventaMostrada == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("No hay una venta creada.")
                        }
                    } else if (!cargando) {
                        // Pago exitoso (simulado)
                        carritoVM.vaciarCarrito()
                        // Dejamos limpio para la próxima compra
                        ventaViewModel.limpiarVentaCreada()
                        onPagoExitoso()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !cargando
            ) {
                if (cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Procesando pago...")
                } else {
                    Text("Pagar")
                }
            }
        }
    }
}
