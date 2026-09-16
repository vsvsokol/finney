package ru.finney.pet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import ru.finney.pet.navigation.FinneyNavHost
import ru.finney.pet.ui.theme.FinneyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinneyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FinneyNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
