package ru.finney.pet.ui.theme

import androidx.compose.ui.graphics.Color

// Палитра из кадра айдентики в Figma. Значения менять только вместе с макетом:
// на этих шести цветах держится весь интерфейс, случайный оттенок сразу видно.

/** Обводка, текст и основной цвет. Обводка есть почти у всего — отсюда он самый частый. */
val FinneyInk = Color(0xFF382C92)

/** Заливка кнопок и акцентов. */
val FinneyPeach = Color(0xFFFA987C)

/** Ошибка. */
val FinneyPink = Color(0xFFF66A85)

/** Успех. */
val FinneyGreen = Color(0xFF86CC8E)

/** Предупреждение. */
val FinneyBlue = Color(0xFF67A6DC)

/** Деньги, награда, достижения. */
val FinneyYellow = Color(0xFFF7DC8B)

// Подложки. В макете панели лежат на кремовом, а не на белом: белый рядом
// с насыщенной обводкой выглядит резко.

/** Фон экрана и панелей. */
val FinneyCream = Color(0xFFFDF0D5)

/** Фон поля ввода и вдавленных областей — на тон светлее кремового. */
val FinneySand = Color(0xFFFCE7BE)

/** Блик в левом верхнем углу кнопок. Белый с прозрачностью, чтобы работать на любой заливке. */
val FinneyGlare = Color(0x99FFFFFF)
