
package com.example.frutosapp

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController

import com.example.frutosapp.api.RetroFistCliente
import com.example.frutosapp.navigation.AppNavHost
import com.example.frutosapp.ui.theme.FrutosAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ==========================================
        // INICIALIZAR SESIÓN PERSISTENTE
        // ==========================================
        // Permite que Retrofit acceda al
        // almacenamiento cifrado del refresh token.
        //
        // Se ejecuta antes de crear las pantallas.
        // ==========================================

        RetroFistCliente.inicializar(applicationContext)

        // ==========================================
        // INTERFAZ PRINCIPAL
        // ==========================================

        setContent {

            FrutosAppTheme {

                val navController = rememberNavController()

                val sessionVM: SesionViewModel = viewModel()

                AppNavHost(
                    navController = navController,
                    sessionVM = sessionVM
                )
            }
        }
    }
}
