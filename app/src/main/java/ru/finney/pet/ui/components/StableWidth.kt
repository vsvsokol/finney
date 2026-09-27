package ru.finney.pet.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import ru.finney.pet.ui.theme.FinneyInk

// Число меняется — соседи стоять на месте. Когда «5 / 50» становилось «15 / 50»,
// подпись раздавалась вширь и сжимала полосу рядом: шкала дёргалась на каждом
// нажатии. Поэтому место под меняющийся текст отмеряется заранее по самому
// широкому значению, которое там может оказаться, а сам текст стоит внутри.
//
// Цифры у Glina разной ширины, так что одной строки-образца мало: к ней в пару
// берётся та же строка, где все цифры — «8», самая широкая из них.

/** Строка-образец, где каждая цифра заменена самой широкой. */
private fun widen(sample: String): String = sample.map { if (it.isDigit()) '8' else it }.joinToString("")

/**
 * Место под текст шириной с самый широкий из [samples]. Образцы невидимы и
 * не читаются TalkBack'ом; [content] — настоящее содержимое, по центру.
 */
@Composable
fun ReserveWidth(
    vararg samples: String,
    sample: @Composable (String) -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = contentAlignment) {
        Box(Modifier.alpha(0f).clearAndSetSemantics { }) {
            samples.forEach { s ->
                sample(s)
                sample(widen(s))
            }
        }
        content()
    }
}

/** Обычный текст, место под который отмерено по [widest]. */
@Composable
fun StableText(
    text: String,
    widest: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = FinneyInk,
) {
    ReserveWidth(widest, sample = { Text(it, style = style) }, modifier = modifier) {
        Text(text, style = style, color = color)
    }
}

/** Текст с контуром, место под который отмерено по [widest]. */
@Composable
fun StableOutlinedText(text: String, widest: String, style: TextStyle, modifier: Modifier = Modifier) {
    ReserveWidth(widest, sample = { OutlinedText(it, style = style) }, modifier = modifier) {
        OutlinedText(text, style = style)
    }
}
