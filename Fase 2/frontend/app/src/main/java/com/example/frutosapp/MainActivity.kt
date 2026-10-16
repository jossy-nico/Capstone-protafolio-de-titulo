package com.example.frutosapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.frutosapp.navigation.AppNavHost
import androidx.navigation.compose.rememberNavController
import com.example.frutosapp.ui.theme.FrutosAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
