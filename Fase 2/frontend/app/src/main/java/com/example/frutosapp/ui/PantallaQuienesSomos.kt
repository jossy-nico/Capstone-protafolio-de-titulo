package com.example.frutosapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.frutosapp.R


data class MiembroEquipo(
    val nombre: String,
    val puesto: String,
    val avatarResId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaQuienesSomos(onNavigateBack: () -> Unit) {
    val equipo = listOf(
        MiembroEquipo("Joselyn Gallardo", "Contacto Principal", R.drawable.mujer)
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiénes Somos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 1. Sección Logo y Misión/Visión
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo de Natur Cosecha Seca",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Misión y Visión", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Misión: Ofrecer a nuestros clientes los frutos secos de la más alta calidad, seleccionados cuidadosamente y procesados bajo estrictos estándares.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Visión: Ser la marca líder y referente en el mercado de frutos secos a nivel nacional.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 2. Sección Equipo
            item {
                SeccionCard(titulo = "Nuestro Equipo") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Fila que contiene la descripción y los avatares
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Descripción a la izquierda
                            Text(
                                "El equipo de Natur Cosecha Seca está compuesto por un grupo de profesionales apasionados y dedicados.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f) // Ocupa el espacio disponible
                            )

                            // Fila con los avatares a la derecha
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                equipo.forEach { miembro ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = miembro.avatarResId),
                                            contentDescription = "Foto de ${miembro.nombre}",
                                            modifier = Modifier
                                                .size(60.dp) // Tamaño del avatar reducido
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Text(miembro.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Text(miembro.puesto, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SeccionCard(titulo = "Natur Cosecha Seca") {
                    Image(
                        painter = painterResource(id = R.drawable.personas),
                        contentDescription = "Foto del equipo de trabajo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            //  Sección Contacto
            item {
                SeccionCard(titulo = "Información de Contacto") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Teléfono: +56 9 7689 5436", style = MaterialTheme.typography.bodyLarge)
                        Text("Correo Electrónico: naturcosecha@frutosecos.cl", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun SeccionCard(
    titulo: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
