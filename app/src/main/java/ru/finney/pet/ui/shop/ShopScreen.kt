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

        CoinAmount(amount = state.balance)

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
                reason = "Сначала составь план — потом покупки",
                actionLabel = "К плану расходов",
                onAction = onOpenBudget,
            )
        }

        ShopSection(
            title = "Нужное",
            planLeft = state.needsLeft,
            previews = state.needs,
            selectedId = state.selectedId,
            block = block,
            onSelect = onSelect,
            onBuy = onBuy,
        )
        ShopSection(
            title = "Хочется",
            planLeft = state.wantsLeft,
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
    planLeft: Int?,
    previews: List<PurchasePreview>,
    selectedId: String?,
    block: CareBlock?,
    onSelect: (String) -> Unit,
    onBuy: (String) -> Unit,
) {
    // Сколько осталось по плану на эту категорию — числом с монеткой. Что в
    // категории лежит, видно по картинкам, подпись «Еда и мытьё» убрана.
    planLeft?.let {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("по плану осталось", style = MaterialTheme.typography.bodyLarge, color = FinneyInk)
            CoinAmount(amount = it, coinSize = 22.dp)
        }
    }
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
