package ru.finney.pet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import ru.finney.pet.navigation.FinneyNavHost
import ru.finney.pet.ui.sound.LocalSounds
import ru.finney.pet.ui.theme.FinneyTheme

class MainActivity : ComponentActivity() {

    private val sounds by lazy { (application as FinneyApplication).container.sounds }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalSounds provides sounds) {
                FinneyTheme {
                    // Отступы под системные панели Scaffold не даёт: каждый экран
                    // ставит их сам и только интерфейсу, а фон и комната уходят
                    // под строку состояния и навигацию до самого края.
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    ) { innerPadding ->
                        FinneyNavHost(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }

    // Свёрнутое приложение молчит: и музыка, и сопение во сне.
    override fun onStart() {
        super.onStart()
        sounds.resume()
    }

    override fun onStop() {
        sounds.pause()
        super.onStop()
    }
}
