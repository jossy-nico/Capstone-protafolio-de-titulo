package com.example.frutosapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.frutosapp.ProductoViewModel
import com.example.frutosapp.model.Categoria
import com.example.frutosapp.ui.components.BarraNavegacionInferior

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCategoriasCliente(
    vm: ProductoViewModel,
    onCategoriaClick: (Categoria) -> Unit,
    onNavigateToInicio: () -> Unit,
    onNavigateToProductos: () -> Unit,
    onNavigateToCategorias: () -> Unit,
    onNavigateToCuenta: () -> Unit
) {

    val categorias by vm.categorias.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorías") }
            )
        },

        bottomBar = {
            BarraNavegacionInferior(
                rutaActual = "categorias",
                onInicioClick = onNavigateToInicio,
                onProductosClick = onNavigateToProductos,
                onCategoriasClick = onNavigateToCategorias,
                onCuentaClick = onNavigateToCuenta
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(
                items = categorias,
                key = { it.idCategoria }
            ) { categoria ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onCategoriaClick(categoria)
                        }
                ) {
                    Text(
                        text = categoria.nomCategoria,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
    }
}