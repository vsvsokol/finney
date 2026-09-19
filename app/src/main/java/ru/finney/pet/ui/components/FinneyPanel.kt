package ru.finney.pet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
// Ключевая деталь, снятая с эталона: заголовок **висит поверх верхнего края**,
// а рамка под ним идёт **сплошной**. Раньше здесь была кремовая подложка,
// разрывавшая рамку, — в макете разрыва нет, и панель читается как цельная
// коробка с наклейкой сверху.
//
// Второе: текст внутри панели в эталоне **без контура**, цветом ink. Контур
// только у заголовка. Не заворачивать содержимое в OutlinedText — надписи
// станут нечитаемыми в мелком кегле.

/**
 * Карточка с заголовком поверх рамки.
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
    // Заголовок налезает на рамку примерно наполовину своей высоты. Отступ сверху
    // отдан ему, поэтому содержимое панели начинается ниже и под него не подлезает.
    val titleOverlap = 22.dp

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = titleOverlap)
                .clip(RoundedCornerShape(RadiusPanel))
                .background(FinneyCream)
                .border(StrokeBold, FinneyInk, RoundedCornerShape(RadiusPanel))
                .padding(start = 20.dp, end = 20.dp, top = titleOverlap + 8.dp, bottom = 20.dp),
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

        // Заголовок поверх всего: рисуется последним, поэтому ложится на рамку,
        // а не под неё. Собственной подложки у него нет — контур и так отделяет
        // буквы от линии рамки, как в эталоне.
        OutlinedText(title, style = MaterialTheme.typography.headlineMedium)
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
