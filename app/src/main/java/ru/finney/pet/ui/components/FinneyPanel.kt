package ru.finney.pet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.finney.pet.ui.theme.FinneyCream
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneyTheme
import ru.finney.pet.ui.theme.RadiusPanel
import ru.finney.pet.ui.theme.StrokeBold

// Панель из макета: кремовая карточка с синей обводкой и крупным заголовком.
// Так нарисованы «Условия», «История», «Магазин», «Настройки» — один компонент
// на все диалоги и разделы.
//
// Заголовок стоит **над рамкой, с зазором**, а не на ней. В кадре кита рамка
// проходит через середину букв, но на устройстве при кегле 28 это читалось
// иначе: буквы садились нижним краем ровно на линию (замер — рамка на 0.92
// высоты букв), и заголовок выглядел придавленным. Решено дать ему воздух снизу.
//
// Рамка по-прежнему **сплошная**: кремовой подложки, разрывающей линию,
// здесь нет и не было.
//
// Текст внутри панели в эталоне **без контура**, цветом ink. Контур только
// у заголовка. Не заворачивать содержимое в OutlinedText — надписи станут
// нечитаемыми в мелком кегле.

/**
 * Карточка с заголовком над рамкой.
 *
 * [onClose] — если задан, внизу появится кнопка «Закрыть»: во всех четырёх
 * панелях эталона она есть, и без неё диалог не закрыть с сенсорного экрана.
 */
@Composable
fun FinneyPanel(
    title: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    closeText: String = "Закрыть",
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Отдельного зазора нет намеренно: у Glina межстрочный интервал крупнее
        // обычного (см. Type.kt), и буквы занимают верхние 60% строки. Нижняя
        // часть строки и есть зазор до рамки — около 13 dp, и он растёт вместе
        // с кеглем, если в системе включён крупный шрифт. Постоянный отступ
        // в dp при крупном шрифте снова посадил бы буквы на линию.
        OutlinedText(title, style = MaterialTheme.typography.headlineMedium)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RadiusPanel))
                .background(FinneyCream)
                .border(StrokeBold, FinneyInk, RoundedCornerShape(RadiusPanel))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()

            onClose?.let {
                FinneyButton(
                    text = closeText,
                    onClick = it,
                    fillWidth = false,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFEDCD)
@Composable
private fun FinneyPanelPreview() {
    FinneyTheme {
        Column(
            modifier = Modifier.background(FinneyCream).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FinneyPanel(title = "Настройки", onClose = {}) {
                Text(
                    "Звук и анимации",
                    style = MaterialTheme.typography.bodyLarge,
                    color = FinneyInk,
                )
                FinneyButton("Руководство", onClick = {})
                FinneySlider(value = 0.4f, onValueChange = {}, label = "Громкость")
            }
        }
    }
}
