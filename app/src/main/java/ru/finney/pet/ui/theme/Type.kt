package ru.finney.pet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

// Кегли подобраны под ТЗ п. 3.6: основной текст не мельче 16 sp, ничего мельче 12 sp
// в интерфейсе нет вообще. Шрифт пока системный — фирменный из макета в репозиторий
// не принесли, см. NOTES.md, раздел «Дизайн».

/**
 * Толщина обводки текста по правилу из макета: 15 при кегле 200.
 * Для 16 sp получается 1.2 dp — так надписи на кнопках держат контур и на мелком тексте.
 *
 * У стиля без явного кегля `fontSize.value` — NaN, и обводка ушла бы в NaN, а текст
 * пропал бы с экрана. На такой случай берётся кегль основного текста.
 */
fun stroke(fontSize: TextUnit): Dp {
    val size = if (fontSize.isSpecified) fontSize.value else 16f
    return (15f * size / 200f).dp
}

private val Display = FontFamily.Default

val Typography = Typography(
    // Заголовки панелей: «Условия», «Магазин», «Настройки».
    headlineLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    // Надписи на кнопках.
    titleLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    // Основной текст. Минимум по ТЗ п. 3.6 — с него и начинаем.
    bodyLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    // Подписи под иконками и суммами. Ниже 14 sp не опускаемся.
    labelLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)
