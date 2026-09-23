package ru.finney.pet.ui.pet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.ui.theme.FinneyTheme

private val MoodLabels = mapOf(
    PetMood.HAPPY to "Радость",
    PetMood.SAD to "Грусть",
    PetMood.DIRTY to "Грязный",
    PetMood.SLEEP to "Сон",
)

/**
 * Черновой экран для подбора анимаций: питомец, переключатель выражений
 * и тап по персонажу, запускающий радость. В игру не входит — удалить,
 * когда анимации переедут на настоящий главный экран.
 */
@Composable
fun PetLabScreen(modifier: Modifier = Modifier) {
    var character by remember { mutableStateOf(PetCharacter.PUSHISTIK) }
    var mood by remember { mutableStateOf(PetMood.HAPPY) }
    val animation = rememberPetAnimation()
    val pose = rememberPoseProvider(animation)
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PetView(
            character = character,
            mood = mood,
            pose = pose,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        mood = PetMood.HAPPY
                        animation.playJoy()
                    },
                ),
        )

        Text(
            text = "Нажми на питомца",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) {
            PetCharacter.entries.forEach { entry ->
                FilterChip(
                    selected = entry == character,
                    onClick = { character = entry },
                    label = { Text(entry.name) },
                    shape = FilterChipDefaults.shape,
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PetMood.entries.forEach { entry ->
                FilterChip(
                    selected = entry == mood,
                    onClick = { mood = entry },
                    label = { Text(MoodLabels.getValue(entry)) },
                    shape = FilterChipDefaults.shape,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PetLabScreenPreview() {
    FinneyTheme { PetLabScreen() }
}
