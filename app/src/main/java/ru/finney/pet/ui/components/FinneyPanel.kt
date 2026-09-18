package ru.finney.pet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyTheme

// Панель из макета: кремовая карточка с синей обводкой и крупным заголовком,
// который «сидит» на верхней грани. Так нарисованы «Условия», «История»,
// «Магазин», «Настройки» — один компонент на все диалоги и разделы.

private val OutlineWidth = 3.dp

/**
 * Карточка с заголовком поверх рамки.
 *
 * Заголовок вынесен из потока содержимого и наложен на границу — иначе
 * пришлось бы держать одинаковый отступ сверху на каждом экране вручную.
 */
@Composable
fun FinneyPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // Верхний отступ — под заголовок, который ляжет поверх рамки.
                .padding(top = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = FinneyCream,
            border = BorderStroke(OutlineWidth, FinneyInk),
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }

        // Подложка под заголовком того же цвета, что панель: она разрывает рамку,
        // и надпись читается как врезанная, а не лежащая на линии.
        Box(
            modifier = Modifier.background(FinneyCream).padding(horizontal = 12.dp),
        ) {
            OutlinedText(title, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDF0D5)
@Composable
private fun FinneyPanelPreview() {
    FinneyTheme {
        Column(
            modifier = Modifier.background(FinneyCream).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FinneyPanel(title = "Настройки") {
                Text("Звук и анимации", style = MaterialTheme.typography.bodyLarge)
                FinneyButton("Руководство", onClick = {})
                FinneyButton("Закрыть", onClick = {})
            }
        }
    }
}
