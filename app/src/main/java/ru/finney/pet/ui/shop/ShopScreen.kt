package ru.finney.pet.ui.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finney.pet.domain.game.PurchasePreview
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.ui.components.CoinAmount
import ru.finney.pet.ui.components.FinneyButton
import ru.finney.pet.ui.components.FinneyScreen
import ru.finney.pet.ui.components.OutlinedText
import ru.finney.pet.ui.components.FeedbackDialog
import ru.finney.pet.ui.room.CareBlock
import ru.finney.pet.ui.room.CareOption
import ru.finney.pet.ui.room.CarePanel
import ru.finney.pet.ui.theme.FinneyInk

/**
 * Магазин: открывается кнопкой с деньгами на главном (так в макете — «при нажатии
 * открывается магазин + история трат»).
 *
 * Две панели — «Нужное» и «Хочется»: ребёнок видит категорию раньше цены.
 * Строки и подтверждение — та же [CarePanel], что у кормления и мытья.
 */
@Composable
fun ShopScreen(
    onBack: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: ShopViewModel = viewModel(factory = ShopViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        ShopUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FinneyInk)
        }

        is ShopUiState.Ready -> ShopContent(
            state = s,
            onSelect = viewModel::select,
            onBuy = viewModel::buy,
            onDismiss = viewModel::dismissFeedback,
            onBack = onBack,
            onOpenBudget = onOpenBudget,
            onOpenHistory = onOpenHistory,
        )
    }
}

@Composable
private fun ShopContent(
    state: ShopUiState.Ready,
    onSelect: (String) -> Unit,
    onBuy: (String) -> Unit,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    FinneyScreen(
        scrollable = true,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        bottom = { FinneyButton(text = "Назад", onClick = onBack) },
    ) {
        OutlinedText("Магазин", style = MaterialTheme.typography.headlineLarge)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("У тебя:", style = MaterialTheme.typography.titleMedium, color = FinneyInk)
            CoinAmount(amount = state.balance)
        }

        FeedbackDialog(
            feedback = state.feedback,
            rejection = state.rejection,
            onDismiss = onDismiss,
            actionLabel = if (state.rejection is Rejection.PlanNotConfirmed) "К плану расходов" else null,
            onAction = onOpenBudget,
        )

        val block = if (state.canBuy) {
            null
        } else {
            CareBlock(
                reason = "Сначала распредели деньги в плане — после этого можно покупать",
                actionLabel = "К плану расходов",
                onAction = onOpenBudget,
            )
        }

        ShopSection(
            title = "Нужное",
            hint = state.needsLeft?.let { "По плану на нужное осталось $it" }
                ?: "Еда и уход — без них питомцу плохо",
            previews = state.needs,
            selectedId = state.selectedId,
            block = block,
            onSelect = onSelect,
            onBuy = onBuy,
        )
        ShopSection(
            title = "Хочется",
            hint = state.wantsLeft?.let { "По плану на желаемое осталось $it" }
                ?: "Радует, но можно и без этого",
            previews = state.wants,
            selectedId = state.selectedId,
            block = block,
            onSelect = onSelect,
            onBuy = onBuy,
        )

        FinneyButton(text = "История трат", onClick = onOpenHistory)
    }
}

@Composable
private fun ShopSection(
    title: String,
    hint: String,
    previews: List<PurchasePreview>,
    selectedId: String?,
    block: CareBlock?,
    onSelect: (String) -> Unit,
    onBuy: (String) -> Unit,
) {
    Text(hint, style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
    CarePanel(
        title = title,
        options = previews.map { CareOption(it.item, it, isSelected = it.item.id == selectedId) },
        onPick = onSelect,
        onConfirm = onBuy,
        onDismiss = null,
        block = block,
        allowShortage = true,
    )
}
