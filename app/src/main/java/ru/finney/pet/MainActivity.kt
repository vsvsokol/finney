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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import ru.finney.pet.navigation.FinneyNavHost
import ru.finney.pet.ui.motion.AppMotion
import ru.finney.pet.ui.motion.LocalAnimations
import ru.finney.pet.ui.sound.LocalSounds
import ru.finney.pet.ui.theme.FinneyTheme

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as FinneyApplication).container }
    private val sounds by lazy { container.sounds }

    /** Скорость анимаций окна: 0 — выключены в настройках (ТЗ п. 3.6). */
    private val motion by lazy { AppMotion(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Свой рекомпозер окна — такой же, как по умолчанию, но скорость анимаций берёт из
        // [motion]: так выключатель действует на все анимации сразу, без правок в экранах.
        val recomposer = window.decorView.createLifecycleAwareWindowRecomposer(motion, lifecycle)
        setContent(parent = recomposer) {
            val settings by sounds.settings.collectAsState()
            SideEffect { motion.enabled = settings.animations }
            CompositionLocalProvider(LocalSounds provides sounds, LocalAnimations provides settings.animations) {
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
        motion.refreshSystemScale()
        sounds.resume()
        // Заглянули — следующее напоминание не раньше чем через сутки (в демо — минуты).
        container.notifications.appOpened()
    }

    override fun onStop() {
        sounds.pause()
        super.onStop()
    }
}
