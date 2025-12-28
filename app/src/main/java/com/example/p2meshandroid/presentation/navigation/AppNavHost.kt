package com.example.p2meshandroid.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.p2meshandroid.presentation.home.HomeScreen
import com.example.p2meshandroid.presentation.mesh.MeshScreen
import com.example.p2meshandroid.presentation.scan.ScanScreen
import com.example.p2meshandroid.presentation.settings.SettingsScreen
import com.example.p2meshandroid.presentation.wallet.WalletViewModel
import com.example.p2meshandroid.ui.theme.DarkBackground

@Composable
fun AppNavHost(
    walletViewModel: WalletViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        modifier = modifier,
        containerColor = DarkBackground,
        bottomBar = {
            BottomNavBar(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Route.Home.route) {
                HomeScreen(walletViewModel = walletViewModel)
            }

            composable(Route.Mesh.route) {
                MeshScreen()
            }

            composable(Route.Scan.route) {
                ScanScreen(walletViewModel = walletViewModel)
            }

            composable(Route.Settings.route) {
                SettingsScreen(walletViewModel = walletViewModel)
            }
        }
    }
}
