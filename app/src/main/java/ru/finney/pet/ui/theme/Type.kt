package ru.finney.pet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import ru.finney.pet.R

// Кегли подобраны под ТЗ п. 3.6: основной текст не мельче 16 sp, ничего мельче 12 sp
// в интерфейсе нет вообще.
//
// Glina на треть уже Roboto при той же высоте букв (замер: фраза «нужное и желаемое» —
// 673 против 925 у Arial при кегле 100). Поэтому текст того же кегля смотрится мельче,
// и кегли подняты на ступень: основной 19 sp, мелкие подписи 16 sp.

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

/**
 * Фирменный шрифт макета — **Glina Script em**, лицензия SIL OFL 1.1
 * (текст — `design/GLINA-SCRIPT-OFL.txt`). Кириллица и цифры покрыты целиком.
 *
 * **Жирных начертаний в семье нет** — только Thin (100) и Regular (400).
 * Поэтому ниже нигде не стоит `FontWeight.Bold` или `Black`: их Android
 * подделывал бы синтетически, размазывая буквы, а весь «мультяшный» вид
 * держится как раз на чистом контуре. Вес набирается кеглем и обводкой
 * ([stroke]), как и сделано в макете.
 */
private val Display = FontFamily(Font(R.font.glina_script))

// Межстрочное расстояние с запасом в ~1.35 кегля: у Glina круглые буквы выше,
// чем у Roboto, под который эти числа подбирались раньше, и на прежних значениях
// у заголовков срезало верх. Обводка текста тоже уходит наружу — ей нужно место.

val Typography = Typography(
    // Заголовки панелей: «Условия», «Магазин», «Настройки».
    headlineLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 46.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 38.sp,
    ),
    // Надписи на кнопках.
    titleLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    // Основной текст. Минимум по ТЗ п. 3.6 — с него и начинаем.
    bodyLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 19.sp,
        lineHeight = 27.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 25.sp,
    ),
    // Подписи под иконками и суммами. Ниже 16 sp не опускаемся.
    labelLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 25.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
)
