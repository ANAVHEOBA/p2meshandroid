package com.example.p2meshandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.p2meshandroid.di.AppContainer
import com.example.p2meshandroid.di.WalletViewModelFactory
import com.example.p2meshandroid.presentation.navigation.AppNavHost
import com.example.p2meshandroid.presentation.wallet.WalletViewModel
import com.example.p2meshandroid.ui.theme.P2meshandroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize DI container with context for storage
        AppContainer.initialize(this)

        enableEdgeToEdge()
        setContent {
            P2meshandroidTheme {
                val walletViewModel: WalletViewModel = viewModel(
                    factory = WalletViewModelFactory()
                )

                AppNavHost(
                    walletViewModel = walletViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                )
            }
        }
    }
}
