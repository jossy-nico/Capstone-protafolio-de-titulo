package com.example.frutosapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.frutosapp.ui.components.BarraNavegacionInferior

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCuenta(
    loginOk: Boolean,
    onLoginClicked: () -> Unit,
    onRegisterClicked: () -> Unit,
    onMisComprasClicked: () -> Unit,
    onMisDatosClicked: () -> Unit,
    onQuienesSomosClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onMisFavoritosClicked: () -> Unit,
    onNavigateToInicio: () -> Unit,
    onNavigateToProductos: () -> Unit,
    onNavigateToCategorias: () -> Unit,
    onNavigateToCuenta: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Cuenta")
                }
            )
        },

        bottomBar = {
            BarraNavegacionInferior(
                rutaActual = "cuenta",
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

            if (!loginOk) {

                item {
                    Text("Bienvenido a Natur Cosecha")
                }

                item {
                    Button(
                        onClick = onLoginClicked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Iniciar sesión")
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onRegisterClicked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Crear cuenta")
                    }
                }

            } else {

                item {
                    OpcionCuenta(
                        texto = "Mis compras",
                        onClick = onMisComprasClicked
                    )
                }

                item {
                    OpcionCuenta(
                        texto = "Mis favoritos",
                        onClick = onMisFavoritosClicked
                    )
                }

                item {
                    OpcionCuenta(
                        texto = "Mis datos",
                        onClick = onMisDatosClicked
                    )
                }

                item {
                    OutlinedButton(
                        onClick = onLogoutClicked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar sesión")
                    }
                }
            }

            item {
                OpcionCuenta(
                    texto = "Quiénes somos",
                    onClick = onQuienesSomosClicked
                )
            }
        }
    }
}

@Composable
private fun OpcionCuenta(
    texto: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(texto)
        }
    }
}