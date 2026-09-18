package ru.finney.pet.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Тема фирменная и одна: ни тёмной, ни dynamicColor. Динамические цвета Android 12+
// перекрасили бы интерфейс в обои пользователя — от айдентики не осталось бы ничего.
// Тёмной темы в макетах нет, придумывать её за дизайнеров не нужно.

private val FinneyColors = lightColorScheme(
    primary = FinneyPeach,
    onPrimary = FinneyInk,
    secondary = FinneyYellow,
    onSecondary = FinneyInk,
    tertiary = FinneyBlue,
    onTertiary = FinneyInk,
    background = FinneyCream,
    onBackground = FinneyInk,
    surface = FinneyCream,
    onSurface = FinneyInk,
    surfaceVariant = FinneySand,
    onSurfaceVariant = FinneyInk,
    error = FinneyPink,
    onError = FinneyInk,
    outline = FinneyInk,
    outlineVariant = FinneyInk,
)

/** Скругления из макета: у кнопок оно почти во всю высоту, у панелей — крупное, но не круг. */
private val FinneyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun FinneyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FinneyColors,
        typography = Typography,
        shapes = FinneyShapes,
        content = content,
    )
}
