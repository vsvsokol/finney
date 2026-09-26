package ru.finney.pet.ui.wardrobe

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.ui.components.FeedbackDialog
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.pet.PetMood
import ru.finney.pet.ui.pet.PetView
import ru.finney.pet.ui.pet.accessoryArt
import ru.finney.pet.ui.pet.rememberPetAnimation
import ru.finney.pet.ui.pet.rememberPoseProvider
import ru.finney.pet.ui.theme.FinneyGreen
import ru.finney.pet.ui.theme.FinneyInk
import ru.finney.pet.ui.theme.FinneySand
import ru.finney.pet.ui.theme.FinneyYellow

// Гардероб — отдельный экран, а не панель поверх комнаты: на главном вокруг питомца
// мебель, шкала, плашки и кнопки, и примерка терялась среди них. Здесь только
// питомец крупно и его вещи. Нажатие на вещь — надеть, на надетую — снять.

@Composable
fun WardrobeScreen(
    onBack: () -> Unit,
    onOpenGoals: () -> Unit,
    viewModel: WardrobeViewModel = viewModel(factory = WardrobeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val s = state) {
        WardrobeUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FinneyInk)
        }
        is WardrobeUiState.Ready -> {
            WardrobeContent(
                state = s,
                onWear = viewModel::wear,
                onTakeOff = viewModel::takeOff,
                onOpenGoals = onOpenGoals,
                onBack = onBack,
            )
            FeedbackDialog(feedback = null, rejection = s.rejection, onDismiss = viewModel::dismissRejection)
        }
    }
}

@Composable
private fun WardrobeContent(
    state: WardrobeUiState.Ready,
    onWear: (String) -> Unit,
    onTakeOff: () -> Unit,
    onOpenGoals: () -> Unit,
    onBack: () -> Unit,
) {
    val animation = rememberPetAnimation()
    FinneyScreen(
        scrollable = true,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        bottom = { FinneyButton(text = "Назад", onClick = onBack) },
    ) {
        OutlinedText("Гардероб", style = MaterialTheme.typography.headlineLarge)

        // Питомец крупно на спокойном круге. Нажатие — подпрыгнет: видно, как сидит шляпа.
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .background(FinneySand)
                .border(3.dp, FinneyInk, CircleShape),
            contentAlignment = Alignment.BottomCenter,
        ) {
            PetView(
                character = state.appearance.character,
                bodyColor = state.appearance.bodyColor,
                accessory = state.worn,
                mood = PetMood.HAPPY,
                pose = rememberPoseProvider(animation),
                modifier = Modifier
                    .size(220.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Подпрыгнуть",
                        onClick = { animation.playJoy() },
                    ),
            )
        }

        // «Без шляпы» — первой плиткой: снять так же просто, как надеть.
        Tile(
            label = "Без шляпы",
            status = if (state.worn == null) "надето" else "снять шляпу",
            selected = state.worn == null,
            enabled = true,
            onClick = onTakeOff,
        )
        state.items.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { entry ->
                    val worn = entry.item.id == state.worn
                    Tile(
                        label = entry.item.label,
                        status = when {
                            worn -> "надето"
                            entry.owned -> "надеть"
                            else -> entry.howToGet.orEmpty()
                        },
                        art = accessoryArt(entry.item.id)?.res,
                        selected = worn,
                        enabled = entry.owned,
                        onClick = { if (worn) onTakeOff() else onWear(entry.item.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (state.items.any { !it.owned }) {
            Text(
                "Шляпы получают за цели в копилке.",
                style = MaterialTheme.typography.bodyLarge,
                color = FinneyInk,
                textAlign = TextAlign.Center,
            )
            FinneyButton(text = "В копилку", onClick = onOpenGoals)
        }
    }
}

/**
 * Плитка вещи. Надетое отмечено и заливкой, и словом «надето» (ТЗ п. 3.6).
 * Чужая вещь бледная и не нажимается, но подписана, как её получить.
 */
@Composable
private fun Tile(
    label: String,
    status: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    art: Int? = null,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) FinneyGreen else if (enabled) FinneyYellow else FinneySand)
            .border(if (selected) 3.dp else 2.dp, FinneyInk, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (art != null) {
            Image(
                painter = painterResource(art),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .alpha(if (enabled) 1f else 0.45f),
            )
        }
        Text(label, style = MaterialTheme.typography.titleMedium, color = FinneyInk, textAlign = TextAlign.Center)
        Text(status, style = MaterialTheme.typography.bodyMedium, color = FinneyInk, textAlign = TextAlign.Center)
    }
}
